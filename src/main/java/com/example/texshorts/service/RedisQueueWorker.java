package com.example.texshorts.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
@Component
@RequiredArgsConstructor
public class RedisQueueWorker implements Runnable {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostDeletionService postDeletionService;

    private static final String DELETE_QUEUE = "delete:post:queue";
    private static final Logger logger = LoggerFactory.getLogger(RedisQueueWorker.class);

    private volatile boolean running = false;
    private Thread workerThread;

    @Override
    public void run() {
        logger.info("RedisQueueWorker 스레드 시작");
        while (running) {
            try {
                String postIdStr = (String) redisTemplate.opsForList().leftPop(DELETE_QUEUE, 10, TimeUnit.SECONDS);
                if (postIdStr != null) {
                    Long postId = Long.parseLong(postIdStr);
                    postDeletionService.deletePostHard(postId);
                }
            } catch (Exception e) {
                logger.error("RedisQueueWorker 오류", e);
            }

            // 대기 상태 진입 시 자동 종료 (큐없음 > 스레드 종료)
            if (redisTemplate.opsForList().size(DELETE_QUEUE) == 0) {
                logger.info("큐 비어 있음, RedisQueueWorker 종료");
                running = false;
                break;
            }
        }
    }

    // 중복 실행 방지 + 수동 트리거
    public synchronized void trigger() {
        if (!running) {
            running = true;
            workerThread = new Thread(this);
            workerThread.setDaemon(true);
            workerThread.start();
            logger.info("RedisQueueWorker 수동 트리거됨");
        } else {
            logger.debug("RedisQueueWorker 이미 실행 중");
        }
    }

    public void stop() {
        running = false;
        logger.info("RedisQueueWorker 수동 종료");
    }
}