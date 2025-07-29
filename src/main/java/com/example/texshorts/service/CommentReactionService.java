package com.example.texshorts.service;

import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.CommentReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.CommentReactionRepository;
import com.example.texshorts.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentReactionService {

    private final CommentReactionRepository commentReactionRepository; // 댓글 리액션 DB 조작용
    private final CommentRepository commentRepository; // 댓글 존재 체크 등
    private final RedisCacheService redisCacheService; // 캐싱 관련

    private final Logger logger = LoggerFactory.getLogger(CommentReactionService.class);


    public void react(Long commentId, User user, ReactionType type) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        Optional<CommentReaction> existing = commentReactionRepository.findByUserAndComment(user, comment);

        if (existing.isPresent()) {
            ReactionType previousType = existing.get().getType();

            if (previousType == type) {
                deleteReaction(existing.get(), commentId, type);
                return;
            }
            deleteReaction(existing.get(), commentId, previousType);
        }
        saveNewReaction(comment, user, type, commentId);
    }

    private void saveNewReaction(Comment comment, User user, ReactionType type, Long commentId) {
        CommentReaction newReaction = new CommentReaction();
        newReaction.setComment(comment);
        newReaction.setUser(user);
        newReaction.setType(type);
        commentReactionRepository.save(newReaction);

        if (type == ReactionType.LIKE) {
            comment.setLikeCount(comment.getLikeCount() + 1);
        } else if (type == ReactionType.DISLIKE) {
            comment.setDislikeCount(comment.getDislikeCount() + 1);
        }

        commentRepository.save(comment); // 변경사항 DB 반영
        redisCacheService.incrementPostReactionCount(commentId, type);
    }

    private void deleteReaction(CommentReaction reaction, Long commentId, ReactionType type) {
        commentReactionRepository.delete(reaction);

        Comment comment = reaction.getComment(); // 연관된 댓글

        if (type == ReactionType.LIKE) {
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else if (type == ReactionType.DISLIKE) {
            comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
        }

        commentRepository.save(comment); // 변경사항 DB 반영
        redisCacheService.decrementPostReactionCount(commentId, type);
    }


}
