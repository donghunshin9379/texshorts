package com.example.texshorts.service;

import com.example.texshorts.component.CommentCountFlusher;
import com.example.texshorts.component.PostCreationService;
import com.example.texshorts.component.PostDeletionService;
import com.example.texshorts.component.ViewCountFlusher;
import com.example.texshorts.dto.message.PostCreationMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;
@Component
@RequiredArgsConstructor
public class RedisQueueWorker implements Runnable {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostCreationService postCreationService;
    private final PostDeletionService postDeletionService;
    private final UserInterestTagService userInterestTagService;
    private final CommentCountFlusher commentCountFlusher;
    private final ViewCountFlusher viewCountFlusher;

    private static final String CREATE_QUEUE = "create:post:queue";
    private static final String DELETE_QUEUE = "delete:post:queue";
    private static final String USER_INTEREST_TAG_QUEUE = "update:userInterestTag:queue";
    private static final String COMMENT_COUNT_UPDATE_QUEUE = "update:commentCount:queue";
    private static final String VIEW_COUNT_UPDATE_QUEUE = "update:viewCount:queue";

    private static final Logger logger = LoggerFactory.getLogger(RedisQueueWorker.class);

    private volatile boolean running = false;
    private Thread workerThread;

    @Override
    public void run() {
        logger.info("RedisQueueWorker 스레드 시작");
        while (running) {
            try {
                /** 게시물 생성 큐 처리 */
                Object messageObj = redisTemplate.opsForList().leftPop(CREATE_QUEUE, 1, TimeUnit.SECONDS);
                if (messageObj instanceof PostCreationMessage msg) {
                    postCreationService.createPostFromMessage(msg);
                    continue;
                }

                /** 게시물 삭제 큐 처리 */
                String postIdStr = (String) redisTemplate.opsForList().leftPop(DELETE_QUEUE, 1, TimeUnit.SECONDS);
                if (postIdStr != null) {
                    Long postId = Long.parseLong(postIdStr);
                    postDeletionService.deletePostHard(postId);
                    continue;
                }

                /** 댓글 카운트 갱신 큐 처리 */
                postIdStr = (String) redisTemplate.opsForList().leftPop(COMMENT_COUNT_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (postIdStr != null) {
                    Long postId = Long.parseLong(postIdStr);
                    commentCountFlusher.flushCommentCountToDatabase(postId);
                    continue;
                }

                /** 조회수 갱신 큐 처리 */
                String viewCountPostIdStr = (String) redisTemplate.opsForList().leftPop(VIEW_COUNT_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (viewCountPostIdStr != null) {
                    Long postId = Long.parseLong(viewCountPostIdStr);
                    viewCountFlusher.flushViewCountToDatabase(postId);
                    continue;
                }

                /** 관심태그 갱신 큐 처리 */
                Object msgObj = redisTemplate.opsForList().leftPop(USER_INTEREST_TAG_QUEUE, 1, TimeUnit.SECONDS);
                if (msgObj instanceof Map<?, ?> map) {
                    Long userId = ((Number) map.get("userId")).longValue();
                    String tagName = (String) map.get("tagName");
                    String action = (String) map.get("action");

                    if ("add".equalsIgnoreCase(action)) {
                        userInterestTagService.addUserInterestTag(userId, tagName);
                        logger.info("UserInterestTag 추가 처리: userId={}, tagName={}", userId, tagName);
                    } else if ("remove".equalsIgnoreCase(action)) {
                        userInterestTagService.removeUserInterestTag(userId, tagName);
                        logger.info("UserInterestTag 삭제 처리: userId={}, tagName={}", userId, tagName);
                    } else {
                        logger.warn("알 수 없는 action: {}", action);
                    }
                    continue;
                }

                /** 큐 모두 비어있으면 종료 */
                if (redisTemplate.opsForList().size(CREATE_QUEUE) == 0 &&
                    redisTemplate.opsForList().size(DELETE_QUEUE) == 0 &&
                    redisTemplate.opsForList().size(VIEW_COUNT_UPDATE_QUEUE) == 0 &&
                    redisTemplate.opsForList().size(COMMENT_COUNT_UPDATE_QUEUE) == 0 &&
                    redisTemplate.opsForList().size(USER_INTEREST_TAG_QUEUE) == 0) {
                    logger.info("큐 비어 있음, RedisQueueWorker 종료");
                    running = false;
                    break;
                }
            } catch (Exception e) {
                logger.error("RedisQueueWorker 오류", e);
            }
        }
    }

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