package com.example.texshorts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDeletionQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisQueueWorker redisQueueWorker;

    private static final String DELETE_QUEUE = "delete:post:queue";

    // 큐 요청 (큐워커 트리거)
    public void enqueuePostForDeletion(Long postId) {
        redisTemplate.opsForList().rightPush(DELETE_QUEUE, String.valueOf(postId));
        redisQueueWorker.trigger();
    }

}

