package com.example.texshorts.service;

import com.example.texshorts.dto.message.PostCreationMessage;
import com.example.texshorts.dto.message.UserInterestTagQueueMessage;
import com.example.texshorts.dto.message.ViewHistorySaveMessage;
import com.example.texshorts.entity.TagActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


@Service
@RequiredArgsConstructor
public class RequestRedisQueue {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisQueueWorker redisQueueWorker;
    private final RedisCacheService redisCacheService;

    private static final String CREATE_QUEUE = "create:post:queue";
    private static final String DELETE_QUEUE = "delete:post:queue";
    private static final String LIKE_COUNT_UPDATE_QUEUE = "update:like_count:queue";
    private static final String COMMENT_COUNT_UPDATE_QUEUE = "update:commentCount:queue";
    private static final String VIEW_COUNT_UPDATE_QUEUE = "update:viewCount:queue";
    private static final String USER_INTEREST_TAG_QUEUE = "update:userInterestTag:queue";
    private static final String VIEW_HISTORY_SAVE_QUEUE = "update:viewHistorySave:queue";


    private static final Logger logger = LoggerFactory.getLogger(RequestRedisQueue.class);


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

    // 조회수 동기화 큐 요청
    public void enqueueViewCountUpdate(Long postId) {
        redisTemplate.opsForList().rightPush(VIEW_COUNT_UPDATE_QUEUE, String.valueOf(postId));
        redisQueueWorker.trigger();
    }

    // 좋아요 수 갱신 큐 요청
    public void enqueueLikeCountUpdate(Long postId) {
        redisTemplate.opsForList().rightPush(LIKE_COUNT_UPDATE_QUEUE, String.valueOf(postId));
        redisQueueWorker.trigger();
    }

    // 관심태그 갱신 큐 요청 (중복방지)
    public void enqueueUserInterestTagUpdate(Long userId, String tagName, TagActionType action) {
        logger.info("리퀘스트레디스큐 요청 ");
        UserInterestTagQueueMessage message = new UserInterestTagQueueMessage(userId, tagName, action);
        redisTemplate.opsForList().rightPush(USER_INTEREST_TAG_QUEUE, message);
        redisQueueWorker.trigger();
    }

    // 인기 피드 갱신 큐 요청
    public void enqueuePopularFeedRefresh() {
        redisTemplate.opsForList().rightPush("update:popularFeed:queue", "trigger");
        redisQueueWorker.trigger();
    }

    // 게시물 시청기록 저장 큐 요청
    public void enqueueViewHistorySave(Long userId, Long postId) {
        try {
            ViewHistorySaveMessage msg = new ViewHistorySaveMessage(userId, postId);
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(msg);  // 직렬화

            redisTemplate.opsForList().rightPush(VIEW_HISTORY_SAVE_QUEUE, json);  // 문자열로 저장
            logger.info("조회 기록 저장 큐 요청: userId={}, postId={}", userId, postId);
            redisQueueWorker.trigger();
        } catch (Exception e) {
            logger.error("ViewHistorySaveMessage 직렬화 실패", e);
        }
    }















}

