package com.example.texshorts.service;

import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.repository.CommentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final Duration VIEWED_POST_TTL = Duration.ofHours(1);
    /**게시물 피드(캐시) 갱신 주기*/
    public static final Duration POST_LIST_TTL = Duration.ofMinutes(10);
    
    private static final String COMMENT_LIST_KEY_PREFIX = "post:comments:";
    private static final String COMMENT_COUNT_KEY_PREFIX = "post:commentCount:";
    private static final String REPLY_LIST_KEY_PREFIX = "comment:replies:";
    private static final String REPLY_COUNT_KEY_PREFIX = "comment:replyCount:";
    private static final String POST_REACTION_KEY_PREFIX = "post:";
    public static final String LATEST_POST_LIST_KEY_PREFIX = "post:latest:";         // 최신 피드
    public static final String POPULAR_POST_LIST_KEY_PREFIX = "post:popular:"; // 인기 피드
    private static final String VIEWED_USER_POST_PREFIX = "viewed:user:";
    private static final String VIEW_COUNT_PREFIX = "viewCount:";
    private static final String USER_INTEREST_TAGS_PREFIX = "user:interest_tags:"; // 관심태그
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    // 기본 get: 문자열 반환
    public String get(String key) {
        try {
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? val : null;
        } catch (Exception e) {
            logger.warn("Redis get 실패: {}", e.getMessage());
            return null;
        }
    }

//    // JSON -> 객체 역직렬화 (단일 객체)
//    public <T> T getAs(String key, Class<T> clazz) {
//        String json = get(key);
//        if (json == null) return null;
//
//        try {
//            return objectMapper.readValue(json, clazz);
//        } catch (Exception e) {
//            logger.warn("Redis JSON 역직렬화 실패: {}", e.getMessage());
//            return null;
//        }
//    }

    // 역직렬화
    // class기반
    public <T> List<T> getListAs(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (Exception e) {
            logger.warn("Redis JSON List 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    // 제네릭 리스트 역직렬화
    // TypeReference 기반
    public <T> List<T> getListAs(String key, TypeReference<List<T>> typeReference) {
        String json = get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            logger.warn("리스트 역직렬화 실패: {}", e.getMessage());
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


    // === Post View 캐시 관련 ===
    // 게시물 조회 여부
    public boolean hasViewed(Long userId, Long postId) {
        String key = VIEWED_USER_POST_PREFIX + userId + ":posts";
        return redisTemplate.opsForSet().isMember(key, postId.toString());
    }


    // 게시물 조회수 증가 (Redis 카운트 증가)
    public Long  incrementViewCount(Long postId) {
        return increment(VIEW_COUNT_PREFIX + postId);
    }

    // 유저-게시물 조회 이력 (조회수 중복 증가 방지)
    public void cacheViewHistory(Long userId, Long postId) {
        String key = VIEWED_USER_POST_PREFIX + userId + ":posts";
        redisTemplate.opsForSet().add(key, postId.toString());
        redisTemplate.expire(key, VIEWED_POST_TTL);
    }

    // 조회한 게시물 ID 목록 조회
    public Set<String> getViewedPostIdSet(Long userId) {
        String key = VIEWED_USER_POST_PREFIX + userId + ":posts";
        Set<String> members = redisTemplate.opsForSet().members(key);
        return members;
    }

    public Long getViewCount(Long postId) {
        String key = VIEW_COUNT_PREFIX + postId;
        String value = get(key);
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            logger.warn("View count 파싱 실패: {}", e.getMessage());
            return null;
        }
    }


    // === Post Feed 캐시 관련 ===
    /**피드 캐시 get*/
    public List<PostResponseDTO> getCachedPostList(int page, int size, String prefix) {
        String key = prefix + page + ":size:" + size;
        return getListAs(key, new TypeReference<>() {});
    }

    /** 피드 캐시 저장*/
    public void cachePostList(int page, int size, List<PostResponseDTO> posts, String prefix) {
        String key = prefix + page + ":size:" + size;
        setAs(key, posts, POST_LIST_TTL);
    }


    /** 노출피드 중복방지 */
    public void cacheSeenFeedPost(Long userId, Long postId) {
        String key = "feed:seen:" + userId;
        redisTemplate.opsForSet().add(key, postId.toString());
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    public Set<String> getSeenFeedPostIds(Long userId) {
        String key = "feed:seen:" + userId;
        return redisTemplate.opsForSet().members(key);
    }

    public void evictSeenFeedPosts(Long userId) {
        String key = "feed:seen:" + userId;
        redisTemplate.delete(key);
    }


    /** 관심태그 관련 =========================================*/

    // 조회 (캐시 우선, 없으면 DB 조회 후 캐싱)
    public Set<String> getUserInterestTags(Long userId, Supplier<Set<String>> dbLoader) {
        String key = USER_INTEREST_TAGS_PREFIX + userId;
        Set<String> cachedTags = redisTemplate.opsForSet().members(key);
        if (cachedTags != null && !cachedTags.isEmpty()) {
            return cachedTags;
        }

        // DB에서 로드
        Set<String> dbTags = dbLoader.get();
        if (dbTags != null && !dbTags.isEmpty()) {
            redisTemplate.opsForSet().add(key, dbTags.toArray(new String[0]));
            redisTemplate.expire(key, Duration.ofHours(6)); // TTL
        }

        return dbTags;
    }

    // 캐시에 단일 태그 추가
    public void addUserInterestTagToCache(Long userId, String tag) {
        String key = USER_INTEREST_TAGS_PREFIX + userId;
        redisTemplate.opsForSet().add(key, tag);
        redisTemplate.expire(key, Duration.ofHours(6)); // TTL 리셋
    }

    // 캐시에서 단일 태그 제거
    public void removeUserInterestTagFromCache(Long userId, String tag) {
        String key = USER_INTEREST_TAGS_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, tag);
    }

    // 캐시 전체 삭제
    public void evictUserInterestTags(Long userId) {
        String key = USER_INTEREST_TAGS_PREFIX + userId;
        redisTemplate.delete(key);
    }


}
