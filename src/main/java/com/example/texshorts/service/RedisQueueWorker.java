package com.example.texshorts.service;

import com.example.texshorts.component.*;
import com.example.texshorts.dto.CommentListResponseDTO;
import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.message.*;
import com.example.texshorts.entity.TagActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final CommentCreationService commentCreationService;
    private final ReplyCommentCreationService replyCommentCreationService;
    private final CommentDeletionService commentDeletionService;
    private final UserInterestTagWorker userInterestTagWorker;
    private final ViewCountFlusher viewCountFlusher;
    private final PopularFeedRefresher popularFeedRefresher;
    private final ReactionCountFlusher reactionCountFlusher;
    private final ViewHistoryWorker viewHistoryWorker;
    private final CommentService commentService;
    private final RedisCacheService redisCacheService;

    private static final String CREATE_QUEUE = "create:post:queue";
    private static final String DELETE_QUEUE = "delete:post:queue";

    private static final String COMMENT_CREATE_QUEUE = "create:comment:queue";
    private static final String REPLY_COMMENT_CREATE_QUEUE = "create:reply:comment:queue";
    private static final String COMMENT_DELETE_QUEUE = "delete:comment:queue";

    private static final String USER_INTEREST_TAG_QUEUE = "update:userInterestTag:queue";
    private static final String LIKE_COUNT_UPDATE_QUEUE = "update:like_count:queue";
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
            COMMENT_CREATE_QUEUE,
            REPLY_COMMENT_CREATE_QUEUE,
            COMMENT_DELETE_QUEUE,
            USER_INTEREST_TAG_QUEUE,
            LIKE_COUNT_UPDATE_QUEUE,
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
                Object postCreationMessageObj = redisTemplate.opsForList().leftPop(CREATE_QUEUE, 1, TimeUnit.SECONDS);
                if (postCreationMessageObj instanceof PostCreationMessage msg) {
                    postCreationService.createPostFromMessage(msg);
                    continue;
                }

                // 게시물 삭제 큐 처리
                Object postDeleteObj = redisTemplate.opsForList().leftPop(DELETE_QUEUE, 1, TimeUnit.SECONDS);
                if (postDeleteObj instanceof PostDeleteMessage msg) {
                    postDeletionService.deletePostHard(msg);
                    continue;
                }

                // 댓글 생성 큐 처리
                Object commentCreationMessageObj = redisTemplate.opsForList().leftPop(COMMENT_CREATE_QUEUE, 1, TimeUnit.SECONDS);
                if (commentCreationMessageObj instanceof CommentCreationMessage msg) {
                    commentCreationService.createCommentFromMessage(msg);
                    CommentListResponseDTO cachedDTO = redisCacheService.getCachedRootCommentsDTO(msg.getPostId());
                    Long lastCommentId = cachedDTO != null ? cachedDTO.getLastCommentId() : null;

                    // 3️⃣ 새로고침: DB + 캐시 반영
                    List<CommentResponseDTO> combined = commentService.refreshRootComments(msg.getPostId(), lastCommentId);
                    // 4️⃣ 최신 상태로 캐시 업데이트
                    redisCacheService.cacheRootComments(msg.getPostId(), new CommentListResponseDTO(combined));
                    continue;
                }

                // 답글 생성 큐처리
                Object replyCommentCreationMessageObj = redisTemplate.opsForList().leftPop(REPLY_COMMENT_CREATE_QUEUE, 1, TimeUnit.SECONDS);
                if (replyCommentCreationMessageObj instanceof ReplyCommentCreationMessage msg) {
                    replyCommentCreationService.createReplyCommentFromMessage(msg);
                    continue;
                }

                // 댓글 삭제 큐 처리
                Object commentDeleteObj = redisTemplate.opsForList().leftPop(COMMENT_DELETE_QUEUE, 1, TimeUnit.SECONDS);
                if (commentDeleteObj instanceof CommentDeleteMessage msg) {
                    commentDeletionService.deleteCommentHard(msg);
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
                            userInterestTagWorker.addUserInterestTag(userId, tagName);
                            logger.info("UserInterestTag 추가 처리: userId={}, tagName={}", userId, tagName);
                        }
                        case REMOVE -> {
                            userInterestTagWorker.removeUserInterestTag(userId, tagName);
                            logger.info("UserInterestTag 삭제 처리: userId={}, tagName={}", userId, tagName);
                        }
                    }
                }

                // 인기 피드 갱신 큐 처리
                String popularFeedSignal = (String) redisTemplate.opsForList().leftPop(POPULAR_FEED_UPDATE_QUEUE, 1, TimeUnit.SECONDS);
                if (popularFeedSignal != null) {
                    popularFeedRefresher.refreshPopularFeedCache();
                    logger.info("인기 피드 캐시 갱신 완료");
                    continue;
                }

                // 시청 기록 갱신 큐 처리
                Object obj = redisTemplate.opsForList().leftPop(VIEW_HISTORY_SAVE_QUEUE, 1, TimeUnit.SECONDS);
                if (obj instanceof String jsonStr) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ViewHistorySaveMessage viewHistoryMsg = objectMapper.readValue(jsonStr, ViewHistorySaveMessage.class);

                        Long userId = viewHistoryMsg.getUserId();
                        Long postId = viewHistoryMsg.getPostId();

                        viewHistoryWorker.saveViewHistory(userId, postId);
                        logger.info("ViewHistory 저장 처리 완료: userId={}, postId={}", userId, postId);

                    } catch (Exception e) {
                        logger.error("조회 기록 역직렬화 실패", e);
                    }
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