package com.example.texshorts.service;

import com.example.texshorts.dto.message.PostCreationMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequestRedisQueue {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisQueueWorker redisQueueWorker;

    private static final String CREATE_QUEUE = "create:post:queue";
    private static final String DELETE_QUEUE = "delete:post:queue";
    private static final String LIKE_COUNT_UPDATE_QUEUE = "update:like_count:queue";
    private static final String COMMENT_COUNT_UPDATE_QUEUE = "update:commentCount:queue";
    private static final String VIEW_COUNT_UPDATE_QUEUE = "update:viewCount:queue";
    private static final String USER_INTEREST_TAG_QUEUE = "update:userInterestTag:queue";

    private static final Logger logger = LoggerFactory.getLogger(RequestRedisQueue.class);


    // 게시물 생성 큐 요청
    public void enqueuePostCreation(PostCreationMessage msg) {
        redisTemplate.opsForList().rightPush(CREATE_QUEUE, msg);
        logger.info(" 게시물 생성 큐 요청 : {}",msg );
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

    // 조회수 동기화 큐 요청
    public void enqueueViewCountUpdate(Long postId) {
        redisTemplate.opsForList().rightPush(VIEW_COUNT_UPDATE_QUEUE, String.valueOf(postId));
        redisQueueWorker.trigger();
    }

    // 좋아요 수 갱신 큐 요청
    public void enqueueLikeCountUpdate(Long postId) {
        redisTemplate.opsForList().rightPush(LIKE_COUNT_UPDATE_QUEUE, String.valueOf(postId));
        logger.info("좋아요 수 갱신 큐 요청: postId={}", postId);
        redisQueueWorker.trigger();
    }

    // 관심태그 갱신 큐 요청 (관심태그 생성 / 삭제)
    public void enqueueUserInterestTagUpdate(Long userId, String tagName, String action) {
        //DTO 대신 Map 활용
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        message.put("tagName", tagName);
        message.put("action", action);

        redisTemplate.opsForList().rightPush(USER_INTEREST_TAG_QUEUE, message);
        logger.info("UserInterestTag 큐 요청: userId={}, tagName={}, action={}", userId, tagName, action);

        redisQueueWorker.trigger();
    }

    // 인기 피드 갱신 큐 요청
    public void enqueuePopularFeedRefresh() {
        redisTemplate.opsForList().rightPush("update:popularFeed:queue", "trigger");
        redisQueueWorker.trigger();
    }


}

