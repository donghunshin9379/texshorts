package com.example.texshorts.repository;

import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.CommentReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    // 댓글 리액션 유무
    Optional<CommentReaction> findByUserAndComment(User user, Comment comment);

    // 특정 댓글 특정 리액션 카운트
    long countByCommentIdAndType(Long commentId, ReactionType type);
}
