package com.example.texshorts.service;

import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    public Comment createComment(Long postId, Long parentId, User user, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        Comment comment;
        if (parentId == null) {
            comment = Comment.createComment(post, user, content);
        } else {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
            comment = Comment.createReply(parent, user, content);
        }
        return commentRepository.save(comment);
    }

    // 댓글 가져오기 (삭제된 댓글(is_deleted)은 X)
    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdAndParentIsNullAndIsDeletedFalse(postId);
    }

    //댓글 삭제(포함된 답글(자식)까지)
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }
        comment.softDeleteRecursive();
        commentRepository.save(comment);
    }
}