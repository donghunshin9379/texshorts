package com.example.texshorts.service;

import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final RequestRedisQueue requestRedisQueue;

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    /** 댓글 목록 조회 (캐시 적용) */
    public List<CommentResponseDTO> getRootComments(Long postId) {
        List<CommentResponseDTO> cached = redisCacheService.getCachedRootComments(postId);
        if (cached != null) return cached;

        List<CommentResponseDTO> dtoList = commentRepository.findRootCommentDTOs(postId);
        //댓글(부모)의 답글(자식) 채움
        dtoList.forEach(dto -> dto.setReplies(List.of()));

        redisCacheService.cacheRootComments(postId, dtoList);
        return dtoList;
    }

    /** 답글 목록 조회 (캐시 적용) */
    public List<CommentResponseDTO> getReplies(Long parentCommentId) {
        List<CommentResponseDTO> cached = redisCacheService.getCachedReplies(parentCommentId);
        if (cached != null) return cached;

        List<CommentResponseDTO> dtoList = commentRepository.findReplyDTOs(parentCommentId);
        dtoList.forEach(dto -> dto.setReplies(List.of()));

        redisCacheService.cacheReplies(parentCommentId, dtoList);
        return dtoList;
    }

    public int getCommentCountCached(Long postId) {
        return redisCacheService.getRootCommentCount(postId);
    }

    public int getReplyCountCached(Long parentCommentId) {
        return redisCacheService.getReplyCount(parentCommentId);
    }

    /** 댓글 생성 and Count 업데이트( 캐싱 > 큐 ) */
    public Comment createRootComment(Long postId, User userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        Comment comment = Comment.createRootComment(post, userId, content);
        Comment saved = commentRepository.save(comment);

        redisCacheService.incrementRootCommentCount(postId);
        requestRedisQueue.enqueueCommentCountUpdate(postId);
        return saved;
    }

    /** 답글 생성 and Count 업데이트( 캐싱 > 큐 ) */
    public Comment createReply(Long parentCommentId, User userId, String content) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (parent.getParent() != null) {
            throw new IllegalArgumentException("답글에는 답글을 달 수 없습니다.");
        }

        Comment reply = Comment.createReply(parent, userId, content);
        Comment saved = commentRepository.save(reply);

        redisCacheService.incrementReplyCount(parentCommentId);
        return saved;
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        comment.softDeleteRecursive();
        commentRepository.save(comment);

        Long postId = comment.getPost().getId();

        if (comment.getParent() == null) {
            // 루트 댓글 삭제
            redisCacheService.decrementRootCommentCount(postId);
            redisCacheService.evictRootCommentList(postId);
        } else {
            // 답글 삭제
            Long parentId = comment.getParent().getId();
            redisCacheService.decrementReplyCount(parentId);
            redisCacheService.evictReplyList(parentId);
        }
    }


}
