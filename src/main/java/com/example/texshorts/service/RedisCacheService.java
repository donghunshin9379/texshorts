package com.example.texshorts.service;

import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.PostReactionRepository;
import com.example.texshorts.repository.PostRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;      // 기존 JSON/String 캐시용
    private final RedisTemplate<String, Long> redisLongTemplate;    // 숫자(Long) 카운트용
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final Duration COMMENT_TTL = Duration.ofMinutes(3);
    private static final Duration VIEWED_POST_TTL = Duration.ofHours(1);
    public static final Duration POST_LIST_TTL = Duration.ofMinutes(10);
    private static final Duration TAG_NAMES_TTL = Duration.ofHours(1);
    public static final Duration COUNT_TTL = Duration.ofHours(1); // TTL 1시간 통합


    private static final String COMMENT_LIST_KEY_PREFIX = "post:comments:";
    private static final String COMMENT_COUNT_KEY_PREFIX = "post:commentCount:";
    private static final String REPLY_LIST_KEY_PREFIX = "comment:replies:";
    private static final String REPLY_COUNT_KEY_PREFIX = "comment:replyCount:";
    private static final String POST_REACTION_KEY_PREFIX = "post:";
    public static final String POPULAR_POST_LIST_KEY_PREFIX = "post:popular:"; // '인기' 카테고리 예비
    private static final String VIEWED_USER_POST_PREFIX = "viewed:user:";
    private static final String VIEW_COUNT_PREFIX = "viewCount:";
    private static final String USER_INTEREST_TAGS_PREFIX = "user:interest_tags:"; // 관심태그
    private static final String POST_TAG_NAMES_KEY_PREFIX = "post:tags:";
    
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

    // TypeReference 기반 Map 역직렬화
    public <K, V> Map<K, V> getMapAs(String key, TypeReference<Map<K, V>> typeReference) {
        String json = get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            logger.warn("Map 역직렬화 실패: {}", e.getMessage());
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
        Long cached = redisLongTemplate.opsForValue().get(key);
        if (cached != null) return cached.intValue();

        int count = commentRepository.countByPostIdAndParentIsNullAndIsDeletedFalse(postId);
        redisLongTemplate.opsForValue().setIfAbsent(key, (long) count, COUNT_TTL);
        return count;
    }

    public void incrementRootCommentCount(Long postId) {
        String key = COMMENT_COUNT_KEY_PREFIX + postId;
        increment(key);
        // 증감 후 TTL 갱신 (30분)
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    // 예시: 댓글 수 증감 후 TTL 갱신을 추가한 decrementRootCommentCount 메서드
    public void decrementRootCommentCount(Long postId) {
        String key = COMMENT_COUNT_KEY_PREFIX + postId;
        decrement(key);
        // 증감 후 TTL 갱신 (30분)
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    // === 답글 수 관련 ===
    public int getReplyCount(Long parentCommentId) {
        String key = REPLY_COUNT_KEY_PREFIX + parentCommentId;
        Long cached = redisLongTemplate.opsForValue().get(key);
        if (cached != null) return cached.intValue();

        int count = commentRepository.countByParentIdAndIsDeletedFalse(parentCommentId);
        redisLongTemplate.opsForValue().setIfAbsent(key, (long) count, COUNT_TTL);
        return count;
    }

    public void incrementReplyCount(Long parentCommentId) {
        String key = REPLY_COUNT_KEY_PREFIX + parentCommentId;
        increment(key);
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    public void decrementReplyCount(Long parentCommentId) {
        String key = REPLY_COUNT_KEY_PREFIX + parentCommentId;
        decrement(key);
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    // === 댓글 목록 캐싱 ===
    public void cacheRootComments(Long postId, List<CommentResponseDTO> commentList) {
        String key = "post:" + postId + ":comments";
        try {
            setAs(key, commentList, COMMENT_TTL);
        } catch (Exception e) {
            logger.warn("댓글 캐시 저장 실패: {}", e.getMessage());
        }
    }

    // 댓글 추가
    public void appendRootComment(Long postId, CommentResponseDTO dto) {
        String key = "post:" + postId + ":comments";
        try {
            // 기존 리스트 가져오기
            List<CommentResponseDTO> list = getListAs(key, CommentResponseDTO.class);
            if (list == null) list = new ArrayList<>();
            list.add(dto);
            // 리스트 다시 저장
            setAs(key, list, COMMENT_TTL);
            logger.warn("댓글 append 성공: {}", list);
            
        } catch (Exception e) {
            logger.warn("댓글 append 실패: {}", e.getMessage());
        }
    }




    public List<CommentResponseDTO> getRootCommentList(Long postId) {
        List<CommentResponseDTO> list = getListAs("post:" + postId + ":comments", CommentResponseDTO.class);
        return list != null ? list : new ArrayList<>();
    }

    public void evictRootCommentList(Long postId) {
        delete(COMMENT_LIST_KEY_PREFIX + postId);
    }

    // === 답글 목록 캐싱 ===
    public List<CommentResponseDTO> getReplieCommentList(Long parentCommentId) {
        return getListAs(REPLY_LIST_KEY_PREFIX + parentCommentId, CommentResponseDTO.class);
    }

    public void cacheReplies(Long parentCommentId, List<CommentResponseDTO> replies) {
        setAs(REPLY_LIST_KEY_PREFIX + parentCommentId, replies, COMMENT_TTL);
    }

    public void evictReplyList(Long parentCommentId) {
        delete(REPLY_LIST_KEY_PREFIX + parentCommentId);
    }



    // === Post Reaction 캐시 관련 ===
    private String getPostReactionKey(Long postId, ReactionType type) {
        return POST_REACTION_KEY_PREFIX + postId + ":" + type.name().toLowerCase();
    }

    // 게시물(피드) 좋아요 수 get
    public Long getPostReactionCount(Long postId, ReactionType type) {
        String key = getPostReactionKey(postId, type);

        // 캐시 조회
        Long cached = redisLongTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        // DB fallback
        Long count = postReactionRepository.countByPostIdAndType(postId, type);

        redisLongTemplate.opsForValue().setIfAbsent(key, count, COUNT_TTL);

        return count;
    }


    public void incrementPostReactionCount(Long postId, ReactionType type) {
        String key = getPostReactionKey(postId, type);
        increment(key);
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    public void decrementPostReactionCount(Long postId, ReactionType type) {
        String key = getPostReactionKey(postId, type);
        decrement(key);
        redisTemplate.expire(key, DEFAULT_TTL);
    }


    // === Post View 캐시 관련 ===
    // 게시물 조회 여부
    public boolean hasViewed(Long userId, Long postId) {
        String key = VIEWED_USER_POST_PREFIX + userId + ":posts";
        boolean isMember = redisTemplate.opsForSet().isMember(key, postId.toString());
        logger.info("hasViewed - key: {}, postId: {}, isMember: {}", key, postId, isMember);
        return isMember;
    }

    // 게시물 조회 유저 저장(조회수 중복 증가 방지)
    public void cacheViewHistory(Long userId, Long postId) {
        String key = VIEWED_USER_POST_PREFIX + userId + ":posts";
        Long added = redisTemplate.opsForSet().add(key, postId.toString());
        redisTemplate.expire(key, VIEWED_POST_TTL);
        logger.info("cacheViewHistory - key: {}, postId: {}, added: {}", key, postId, added);
    }


//    // 조회한 게시물 ID 목록 조회
//    public Set<String> getViewedPostIdSet(Long userId) {
//        String key = VIEWED_USER_POST_PREFIX + userId + ":posts";
//        Set<String> members = redisTemplate.opsForSet().members(key);
//        return members;
//    }

    // 게시물 조회수 증가 (Redis 카운트 증가)
    public Long incrementViewCount(Long postId) {
        String key = VIEW_COUNT_PREFIX + postId;
        Long val = increment(key);
        redisTemplate.expire(key, DEFAULT_TTL);
        return val;
    }

    // 게시물(피드) 조회수 get
    public Long getViewCount(Long postId) {
        String key = VIEW_COUNT_PREFIX + postId;

        Long cached = redisLongTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        Long count = postRepository.getViewCountByPostId(postId);
        if (count == null) count = 0L;

        redisLongTemplate.opsForValue().setIfAbsent(key, count, COUNT_TTL);
        return count;
    }



    // === Post Feed 캐시 관련 ===
    /** 피드 캐시 조회 */
    public List<PostResponseDTO> getCachedPostList(int page, int size, String prefix) {
        String key = prefix + "page:" + page + ":size:" + size;
        List<PostResponseDTO> cached = getListAs(key, new TypeReference<>() {});
        if (cached == null) {
            logger.info("캐시 비어 있음, key: {}", key);
            return List.of();
        }
        return cached;
    }

    /** 피드 캐시 저장 */
    public void cachePostList(int page, int size, List<PostResponseDTO> posts, String prefix) {
        if (posts == null || posts.isEmpty()) return;
        String key = prefix + "page:" + page + ":size:" + size;
        setAs(key, posts, POST_LIST_TTL);
        logger.info("캐시 저장 성공, key: {}, size: {}", key, posts.size());
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

    /** 피드(게시물) 생성시 카운트 초기화 */
    public void initializePostCounts(Long postId) {
        String viewKey = VIEW_COUNT_PREFIX + postId;
        String commentKey = COMMENT_COUNT_KEY_PREFIX + postId;
        String likeKey = getPostReactionKey(postId, ReactionType.LIKE);

        redisLongTemplate.opsForValue().setIfAbsent(viewKey, 0L);
        redisLongTemplate.opsForValue().setIfAbsent(commentKey, 0L);
        redisLongTemplate.opsForValue().setIfAbsent(likeKey, 0L);
    }



    /** 관심태그 관련 =========================================*/

    // 유저 관심태그 조회 (캐시 우선, 없으면 DB 조회 후 캐싱)
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
    
    /** postId 기준 태그네임 캐시 관련 ========================================*/
    // 태그 목록 캐싱용 get
    public List<String> getTagNamesByPostId(Long postId) {
        String key = POST_TAG_NAMES_KEY_PREFIX + postId;

        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                redisTemplate.delete(key);
            }
        }

        List<String> tagNames = postRepository.findTagNamesByPostId(postId);
        try {
            String json = objectMapper.writeValueAsString(tagNames);
            redisTemplate.opsForValue().set(key, json, TAG_NAMES_TTL);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 시 로그만 남기고 무시
        }
        return tagNames;
    }

    //루트 댓글 ID 리스트 조회
    public List<Long> getRootCommentIds(Long postId) {
        String key = "post:" + postId + ":rootCommentIds";

        List<Long> cached = getListAs(key, Long.class); // 직렬화 활용
        if (cached != null) return cached;

        List<Long> ids = commentRepository.findRootCommentIdsByPostId(postId);
        setAs(key, ids, COMMENT_TTL); // 캐시 저장
        return ids;
    }

    //답글 ID 매핑 조회
    public Map<Long, List<Long>> getReplyIds(Long postId) {
        String key = "post:" + postId + ":replyIds";

        Map<Long, List<Long>> cached = getMapAs(key, new TypeReference<>() {});
        if (cached != null) return cached;

        Map<Long, List<Long>> replyMap = commentRepository.findReplyIdsByPostId(postId);
        setAs(key, replyMap, COMMENT_TTL);
        return replyMap;
    }

    //유저별 좋아요 여부 조회
    public boolean hasUserLiked(Long postId, Long userId) {
        String key = POST_REACTION_KEY_PREFIX + postId + ":likedUserIds";
        Boolean result = redisTemplate.opsForSet().isMember(key, userId.toString());
        if (result != null) return result;

        // DB fallback
        boolean liked = postReactionRepository.existsByPostIdAndUserIdAndType(postId, userId, ReactionType.LIKE); // repository 쿼리 필요
        cacheUserReaction(postId, userId, liked, false); // disliked=false는 임시
        return liked;
    }

    //유저별 싫어요 여부 조회
    public boolean hasUserDisliked(Long postId, Long userId) {
        String key = POST_REACTION_KEY_PREFIX + postId + ":dislikedUserIds";
        Boolean result = redisTemplate.opsForSet().isMember(key, userId.toString());
        if (result != null) return result;

        // DB fallback
        boolean disliked = postReactionRepository.existsByPostIdAndUserIdAndType(postId, userId, ReactionType.DISLIKE);
        cacheUserReaction(postId, userId, false, disliked);
        return disliked;
    }

    // 유저 좋아요/싫어요 캐시 저장
    public void cacheUserReaction(Long postId, Long userId, boolean liked, boolean disliked) {
        String likeKey = POST_REACTION_KEY_PREFIX + postId + ":likedUserIds";
        String dislikeKey = POST_REACTION_KEY_PREFIX + postId + ":dislikedUserIds";

        if (liked) redisTemplate.opsForSet().add(likeKey, userId.toString());
        else redisTemplate.opsForSet().remove(likeKey, userId.toString());

        if (disliked) redisTemplate.opsForSet().add(dislikeKey, userId.toString());
        else redisTemplate.opsForSet().remove(dislikeKey, userId.toString());

        redisTemplate.expire(likeKey, DEFAULT_TTL);
        redisTemplate.expire(dislikeKey, DEFAULT_TTL);
    }

}
