package com.example.texshorts.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
/** Redis큐 요청
 * 큐 요청 없을시 대기 (블로킹)
 * */
@Component
@RequiredArgsConstructor
public class RedisQueueWorker implements Runnable {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostDeletionService postDeletionService;
    private static final String DELETE_QUEUE = "delete:post:queue";
    private volatile boolean running = true;
    private static final Logger logger = LoggerFactory.getLogger(RedisQueueWorker.class);


    @PostConstruct
    public void start() { //Spring 서버 종료시 강제종료
        Thread workerThread = new Thread(this);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /** 
     * 큐 실행
     * 큐 존재 -> 즉시 작업
     * 큐 X -> 최대 10초 대기
     * */ 
    @Override
    public void run() {
        while (running) {
            try {
                logger.info("레디스큐워커 실행");
                Object postIdObj = redisTemplate.opsForList().leftPop(DELETE_QUEUE, 10, TimeUnit.SECONDS);
                if (postIdObj != null) {
                    Long postId = (Long) postIdObj;
                    postDeletionService.deletePostHard(postId);
                }
            } catch (Exception e) {
            }
        }
    }

    // 명시적 종료 stop
    public void stop() {
        logger.info("레디스큐워커 종료");
        running = false;
    }
}
