package com.example.texshorts.service;

import com.example.texshorts.component.*;
import com.example.texshorts.dto.message.PostCreationMessage;
import com.example.texshorts.dto.message.UserInterestTagQueueMessage;
import com.example.texshorts.dto.message.ViewHistorySaveMessage;
import com.example.texshorts.entity.TagActionType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
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
    private final PopularFeedRefresher popularFeedRefresher;
    private final ReactionCountFlusher reactionCountFlusher;
    private final ViewHistoryWorker viewHistoryWorker;

    private static final String CREATE_QUEUE = "create:post:queue";
    private static final String DELETE_QUEUE = "delete:post:queue";
    private static final String USER_INTEREST_TAG_QUEUE = "update:userInterestTag:queue";
    private static final String LIKE_COUNT_UPDATE_QUEUE = "update:like_count:queue";
    private static final String COMMENT_COUNT_UPDATE_QUEUE = "update:commentCount:queue";
    private static final String VIEW_COUNT_UPDATE_QUEUE = "update:viewCount:queue";
    private static final String POPULAR_FEED_UPDATE_QUEUE = "update:popularFeed:queue";
    private static final String VIEW_HISTORY_SAVE_QUEUE = "update:viewHistorySave:queue";


    private static final Logger logger = LoggerFactory.getLogger(RedisQueueWorker.class);

    private volatile boolean running = false;
    private Thread workerThread;

    // 큐 종료용 리스트
    private static final List<String> MONITORED_QUEUES = List.of(
            CREATE_QUEUE,
            DELETE_QUEUE,
            USER_INTEREST_TAG_QUEUE,
            LIKE_COUNT_UPDATE_QUEUE,
            COMMENT_COUNT_UPDATE_QUEUE,
            VIEW_COUNT_UPDATE_QUEUE,
            POPULAR_FEED_UPDATE_QUEUE,
            VIEW_HISTORY_SAVE_QUEUE
    );

    @Override
    public void run() {
        logger.info("RedisQueueWorker 스레드 시작");
        while (running) {
            try {
                // 게시물 생성 큐 처리
                Object messageObj = redisTemplate.opsForList().leftPop(CREATE_QUEUE, 1, TimeUnit.SECONDS);
                if (messageObj instanceof PostCreationMessage msg) {
                    postCreationService.createPostFromMessage(msg);
                    continue;
                }

                // 게시물 삭제 큐 처리
                String postIdStr = (String) redisTemplate.opsForList().leftPop(DELETE_QUEUE, 1, TimeUnit.SECONDS);
                if (postIdStr != null) {
                    Long postId = Long.parseLong(postIdStr);
                    postDeletionService.deletePostHard(postId);
                    continue;
                }

                // 좋아요 카운트 갱신 큐 처리
                String likeCountPostIdStr = (String) redisTemplate.opsForList().leftPop(LIKE_COUNT_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (likeCountPostIdStr != null) {
                    Long postId = Long.parseLong(likeCountPostIdStr);
                    reactionCountFlusher.flushLikeCountToDatabase(postId);
                    logger.info("LikeCount DB 반영 처리 완료: postId={}", postId);
                    continue;
                }

                // 댓글 카운트 갱신 큐 처리
                postIdStr = (String) redisTemplate.opsForList().leftPop(COMMENT_COUNT_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (postIdStr != null) {
                    Long postId = Long.parseLong(postIdStr);
                    commentCountFlusher.flushCommentCountToDatabase(postId);
                    continue;
                }

                // 조회수 갱신 큐 처리
                String viewCountPostIdStr = (String) redisTemplate.opsForList().leftPop(VIEW_COUNT_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (viewCountPostIdStr != null) {
                    Long postId = Long.parseLong(viewCountPostIdStr);
                    viewCountFlusher.flushViewCountToDatabase(postId);
                    continue;
                }

                // 관심태그 갱신 큐 처리
                Object msgObj = redisTemplate.opsForList().leftPop(USER_INTEREST_TAG_QUEUE, 1, TimeUnit.SECONDS);

                if (msgObj instanceof UserInterestTagQueueMessage message) {
                    Long userId = message.getUserId();
                    String tagName = message.getTagName();
                    TagActionType action = message.getAction();

                    switch (action) {
                        case ADD -> {
                            userInterestTagService.addUserInterestTag(userId, tagName);
                            logger.info("UserInterestTag 추가 처리: userId={}, tagName={}", userId, tagName);
                        }
                        case REMOVE -> {
                            userInterestTagService.removeUserInterestTag(userId, tagName);
                            logger.info("UserInterestTag 삭제 처리: userId={}, tagName={}", userId, tagName);
                        }
                    }
                }

                // 인기 피드 캐시 갱신 큐 처리
                String popularFeedSignal = (String) redisTemplate.opsForList().leftPop(POPULAR_FEED_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (popularFeedSignal != null) {
                    popularFeedRefresher.refreshPopularFeedCache();
                    logger.info("인기 피드 캐시 갱신 완료");
                    continue;
                }

                // 조회 기록 저장 큐 처리
                Object viewHistoryMsgObj = redisTemplate.opsForList().leftPop(VIEW_HISTORY_SAVE_QUEUE, 1, TimeUnit.SECONDS);
                if (viewHistoryMsgObj instanceof ViewHistorySaveMessage viewHistoryMsg) {
                    Long userId = viewHistoryMsg.getUserId();
                    Long postId = viewHistoryMsg.getPostId();

                    viewHistoryWorker.saveViewHistory(userId, postId);

                    logger.info("ViewHistory 저장 처리 완료: userId={}, postId={}", userId, postId);
                    continue;
                }

                // 모든 큐 비어 있으면 종료
                boolean allEmpty = MONITORED_QUEUES.stream()
                        .allMatch(queue -> redisTemplate.opsForList().size(queue) == 0);

                if (allEmpty) {
                    logger.info("모든 큐 비어 있음, RedisQueueWorker 종료");
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