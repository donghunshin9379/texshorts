package com.example.texshorts.service;


import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.repository.CommentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private static final String COMMENT_LIST_KEY_PREFIX = "post:comments:";
    private static final String COMMENT_COUNT_KEY_PREFIX = "post:commentCount:";
    private static final String REPLY_LIST_KEY_PREFIX = "comment:replies:";
    private static final String REPLY_COUNT_KEY_PREFIX = "comment:replyCount:";
    private static final String POST_REACTION_KEY_PREFIX = "post:";
    private static final String POST_LIST_KEY_PREFIX = "posts:page:";

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    // 기본 get: 문자열 반환
    public String get(String key) {
        try {
            Object val = redisTemplate.opsForValue().get(key);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            logger.warn("Redis get 실패: {}", e.getMessage());
            return null;
        }
    }

    // JSON -> 객체 역직렬화 (단일 객체)
    public <T> T getAs(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.warn("Redis JSON 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    // JSON -> List<T> 역직렬화
    public <T> List<T> getListAs(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            logger.warn("Redis JSON List 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    // 객체 -> JSON 직렬화 후 저장
    public <T> void setAs(String key, T value) {
        setAs(key, value, DEFAULT_TTL);
    }

    public <T> void setAs(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            logger.warn("Redis JSON 직렬화 실패: {}", e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.warn("Redis delete 실패: {}", e.getMessage());
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            logger.warn("Redis increment 실패: {}", e.getMessage());
            return null;
        }
    }

    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            logger.warn("Redis decrement 실패: {}", e.getMessage());
            return null;
        }
    }

    // === 게시물 리스트 관련 ===
    // 캐시에서 게시물 리스트 조회
    public List<PostResponseDTO> getCachedPostList(int page, int size) {
        String key = POST_LIST_KEY_PREFIX + page + ":size:" + size;
        return getListAs(key, PostResponseDTO.class);
    }

    // 게시물 리스트 캐시 저장
    public void cachePostList(int page, int size, List<PostResponseDTO> posts) {
        String key = POST_LIST_KEY_PREFIX + page + ":size:" + size;
        setAs(key, posts, DEFAULT_TTL);
    }

    // 캐시 갱신(삭제 + 재생성)
    public void updatePostListCache(int page, int size, List<PostResponseDTO> posts) {
        cachePostList(page, size, posts);
    }



    // === 댓글 수 관련 ===
    public int getRootCommentCount(Long postId) {
        String key = COMMENT_COUNT_KEY_PREFIX + postId;
        String cached = get(key);
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException e) {
                logger.warn("댓글 수 캐시 파싱 실패: {}", e.getMessage());
            }
        }
        int count = commentRepository.countByPostIdAndParentIsNullAndIsDeletedFalse(postId);
        setAs(key, String.valueOf(count));
        return count;
    }

    public void incrementRootCommentCount(Long postId) {
        increment(COMMENT_COUNT_KEY_PREFIX + postId);
    }

    public void decrementRootCommentCount(Long postId) {
        decrement(COMMENT_COUNT_KEY_PREFIX + postId);
    }


    // === 답글 수 관련 ===
    public int getReplyCount(Long parentCommentId) {
        String key = REPLY_COUNT_KEY_PREFIX + parentCommentId;
        String cached = get(key);
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException e) {
                logger.warn("답글 수 캐시 파싱 실패: {}", e.getMessage());
            }
        }
        int count = commentRepository.countByParentIdAndIsDeletedFalse(parentCommentId);
        setAs(key, String.valueOf(count));
        return count;
    }

    public void incrementReplyCount(Long parentCommentId) {
        increment(REPLY_COUNT_KEY_PREFIX + parentCommentId);
    }

    public void decrementReplyCount(Long parentCommentId) {
        decrement(REPLY_COUNT_KEY_PREFIX + parentCommentId);
    }


    // === 댓글 목록 캐싱 ===
    public List<CommentResponseDTO> getCachedRootComments(Long postId) {
        return getListAs(COMMENT_LIST_KEY_PREFIX + postId, CommentResponseDTO.class);
    }

    public void cacheRootComments(Long postId, List<CommentResponseDTO> comments) {
        setAs(COMMENT_LIST_KEY_PREFIX + postId, comments);
    }

    public void evictRootCommentList(Long postId) {
        delete(COMMENT_LIST_KEY_PREFIX + postId);
    }


    // === 답글 목록 캐싱 ===
    public List<CommentResponseDTO> getCachedReplies(Long parentCommentId) {
        return getListAs(REPLY_LIST_KEY_PREFIX + parentCommentId, CommentResponseDTO.class);
    }

    public void cacheReplies(Long parentCommentId, List<CommentResponseDTO> replies) {
        setAs(REPLY_LIST_KEY_PREFIX + parentCommentId, replies);
    }

    public void evictReplyList(Long parentCommentId) {
        delete(REPLY_LIST_KEY_PREFIX + parentCommentId);
    }


    // === Post Reaction 캐시 관련 ===
    private String getPostReactionKey(Long postId, ReactionType type) {
        return POST_REACTION_KEY_PREFIX + postId + ":" + type.name().toLowerCase();
    }

    public Long getPostReactionCount(Long postId, ReactionType type, Supplier<Long> dbCountSupplier) {
        String key = getPostReactionKey(postId, type);
        String cached = get(key);
        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException e) {
                logger.warn("Post Reaction count 캐시 파싱 실패: {}", e.getMessage());
            }
        }
        Long count = dbCountSupplier.get();
        setAs(key, String.valueOf(count));
        return count;
    }

    public void incrementPostReactionCount(Long postId, ReactionType type) {
        increment(getPostReactionKey(postId, type));
    }

    public void decrementPostReactionCount(Long postId, ReactionType type) {
        decrement(getPostReactionKey(postId, type));
    }

    public Long getPostReactionCount(Long postId, ReactionType type, Long dbCount) {
        String key = getPostReactionKey(postId, type);
        String cached = get(key);
        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException e) {
                logger.warn("Post Reaction count 파싱 실패: {}", e.getMessage());
            }
        }
        setAs(key, String.valueOf(dbCount));
        return dbCount;
    }
}
