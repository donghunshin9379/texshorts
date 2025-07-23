//package com.example.texshorts.service;
//
//import com.example.texshorts.DTO.CommentResponseDTO;
//import com.example.texshorts.entity.Comment;
//import com.example.texshorts.repository.CommentRepository;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class CommentCache {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final CommentRepository commentRepository;
//
//    private static final Logger logger = LoggerFactory.getLogger(CommentCache.class);
//
//    private static final String COMMENT_LIST_KEY_PREFIX = "post:comments:";       // 댓글 목록
//    private static final String COMMENT_COUNT_KEY_PREFIX = "post:commentCount:";  // 댓글 수
//
//    private static final String REPLY_LIST_KEY_PREFIX = "comment:replies:";       // 답글 목록
//    private static final String REPLY_COUNT_KEY_PREFIX = "comment:replyCount:";   // 답글 수
//
//
//    /** 댓글 캐싱 관련 메소드  */
//    public int getRootCommentCount(Long postId) {
//        String key = COMMENT_COUNT_KEY_PREFIX + postId;
//        Integer cached = (Integer) redisTemplate.opsForValue().get(key);
//        if (cached != null) return cached;
//
//        int count = commentRepository.countByPostIdAndParentIsNullAndIsDeletedFalse(postId);
//        redisTemplate.opsForValue().set(key, count);
//        return count;
//    }
//
//    public void incrementRootCommentCount(Long postId) {
//        redisTemplate.opsForValue().increment(COMMENT_COUNT_KEY_PREFIX + postId);
//    }
//
//    public void decrementRootCommentCount(Long postId) {
//        redisTemplate.opsForValue().decrement(COMMENT_COUNT_KEY_PREFIX + postId);
//    }
//
//    /** 답글 캐싱 관련 메소드  */
//    public int getReplyCount(Long parentCommentId) {
//        String key = REPLY_COUNT_KEY_PREFIX + parentCommentId;
//        Integer cached = (Integer) redisTemplate.opsForValue().get(key);
//        if (cached != null) return cached;
//
//        int count = commentRepository.countByParentIdAndIsDeletedFalse(parentCommentId);
//        redisTemplate.opsForValue().set(key, count);
//        return count;
//    }
//
//    public void incrementReplyCount(Long parentCommentId) {
//        redisTemplate.opsForValue().increment(REPLY_COUNT_KEY_PREFIX + parentCommentId);
//    }
//
//    public void decrementReplyCount(Long parentCommentId) {
//        redisTemplate.opsForValue().decrement(REPLY_COUNT_KEY_PREFIX + parentCommentId);
//    }
//
//
//
//    /** 댓글 목록 캐싱 조회 */
//    @SuppressWarnings("unchecked")
//    public List<CommentResponseDTO> getCachedRootComments(Long postId) {
//        String key = COMMENT_LIST_KEY_PREFIX + postId;
//        return (List<CommentResponseDTO>) redisTemplate.opsForValue().get(key);
//    }
//
//    /** 댓글 목록 캐시 저장 */
//    public void cacheRootComments(Long postId, List<CommentResponseDTO> comments) {
//        String key = COMMENT_LIST_KEY_PREFIX + postId;
//        redisTemplate.opsForValue().set(key, comments);
//    }
//
//    /** 답글 목록 캐시 조회 */
//    // 캐시 조회 메소드
//    @SuppressWarnings("unchecked")
//    public List<CommentResponseDTO> getCachedReplies(Long parentCommentId) {
//        String key = REPLY_LIST_KEY_PREFIX + parentCommentId;
//        return (List<CommentResponseDTO>) redisTemplate.opsForValue().get(key);
//    }
//
//    /** 답글 목록 캐시 저장 */
//    // 캐시 저장 메소드
//    public void cacheReplies(Long parentCommentId, List<CommentResponseDTO> replies) {
//        String key = REPLY_LIST_KEY_PREFIX + parentCommentId;
//        redisTemplate.opsForValue().set(key, replies);
//    }
//
//    /** 댓글 목록 캐시 삭제 */
//    public void evictCommentList(Long postId) {
//        redisTemplate.delete(COMMENT_LIST_KEY_PREFIX + postId);
//    }
//
//    /** 답글 목록 캐시 삭제 */
//    public void evictReplyList(Long parentCommentId) {
//        redisTemplate.delete(REPLY_LIST_KEY_PREFIX + parentCommentId);
//    }
//}
