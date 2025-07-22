package com.example.texshorts.service;

import com.example.texshorts.DTO.CommentResponseDTO;
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
    private final CommentCache commentCache;

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    /** 댓글 목록 조회 (캐시 적용) */
    public List<CommentResponseDTO> getRootComments(Long postId) {
        List<CommentResponseDTO> cached = commentCache.getCachedRootComments(postId);
        if (cached != null) {
            return cached;
        }
        List<Comment> comments = commentRepository.findByPostIdAndParentIsNullAndIsDeletedFalse(postId);
        List<CommentResponseDTO> dtoList = comments.stream()
                .map(comment -> CommentResponseDTO.from(comment, getReplyCountCached(comment.getId())))
                .toList();

        commentCache.cacheRootComments(postId, dtoList);
        return dtoList;
    }

    /** 답글 목록 조회 (캐시 적용) */
    public List<CommentResponseDTO> getReplies(Long parentCommentId) {
        List<CommentResponseDTO> cached = commentCache.getCachedReplies(parentCommentId);
        if (cached != null) {
            return cached;
        }
        List<Comment> replies = commentRepository.findByParentId(parentCommentId);
        List<CommentResponseDTO> dtoList = replies.stream()
                .map(reply -> CommentResponseDTO.from(reply, 0)) // 답글은 대댓글이 없으므로 replyCount = 0
                .toList();

        commentCache.cacheReplies(parentCommentId, dtoList);
        return dtoList;
    }

    public int getCommentCountCached(Long postId) {
        return commentCache.getRootCommentCount(postId);
    }

    public int getReplyCountCached(Long parentCommentId) {
        return commentCache.getReplyCount(parentCommentId);
    }

    // 루트 댓글 생성 시 캐시 무효화
    public Comment createRootComment(Long postId, User user, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        Comment comment = Comment.createComment(post, user, content);
        Comment saved = commentRepository.save(comment);

        commentCache.incrementRootCommentCount(postId);
        commentCache.evictCommentList(postId); // 캐시 무효화

        return saved;
    }

    // 답글 생성 시 캐시 무효화
    public Comment createReply(Long parentCommentId, User user, String content) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (parent.getParent() != null) {
            throw new IllegalArgumentException("답글에는 답글을 달 수 없습니다.");
        }

        Comment reply = Comment.createReply(parent, user, content);
        Comment saved = commentRepository.save(reply);

        commentCache.incrementReplyCount(parentCommentId);
        commentCache.evictReplyList(parentCommentId); // 답글 목록 캐시 무효화
        commentCache.evictCommentList(parent.getPost().getId()); // 루트 댓글 목록 캐시 무효화(필요시)

        return saved;
    }

    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        comment.softDeleteRecursive();
        commentRepository.save(comment);

        Long postId = comment.getPost().getId();

        if (comment.getParent() == null) {
            // 루트 댓글 삭제
            commentCache.decrementRootCommentCount(postId);
            commentCache.evictCommentList(postId);
        } else {
            // 답글 삭제
            Long parentId = comment.getParent().getId();
            commentCache.decrementReplyCount(parentId);
            commentCache.evictReplyList(parentId);
        }
    }
}
