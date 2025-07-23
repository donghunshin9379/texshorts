package com.example.texshorts.service;

import com.example.texshorts.dto.message.PostCreationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestRedisQueue {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisQueueWorker redisQueueWorker;

    private static final String CREATE_QUEUE = "create:post:queue";
    private static final String DELETE_QUEUE = "delete:post:queue";
    private static final String COMMENT_COUNT_UPDATE_QUEUE = "update:commentCount:queue";

    // 게시물 생성 큐 요청
    public void enqueuePostCreation(PostCreationMessage msg) {
        redisTemplate.opsForList().rightPush(CREATE_QUEUE, msg);
        redisQueueWorker.trigger();
    }

    // 게시물 삭제 큐 요청
    public void enqueuePostForDeletion(Long postId) {
        redisTemplate.opsForList().rightPush(DELETE_QUEUE, String.valueOf(postId));
        redisQueueWorker.trigger();
    }

    // 댓글 갯수 조회 큐 요청
    public void enqueueCommentCountUpdate(Long postId) {
        redisTemplate.opsForList().rightPush(COMMENT_COUNT_UPDATE_QUEUE, String.valueOf(postId));
        redisQueueWorker.trigger();
    }


}

