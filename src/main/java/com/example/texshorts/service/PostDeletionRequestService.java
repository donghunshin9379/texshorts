package com.example.texshorts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDeletionRequestService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DELETE_QUEUE = "delete:post:queue";

    public void enqueuePostForDeletion(Long postId) {
        redisTemplate.opsForList().rightPush(DELETE_QUEUE, postId);
    }
}

