package com.example.texshorts.component;

import com.example.texshorts.dto.message.CommentDeleteMessage;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentDeletionService {

    private final CommentRepository commentRepository;

    @Transactional
    public void deleteCommentHard(CommentDeleteMessage msg) {
        Comment comment = commentRepository.findById(msg.getCommentId())
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!comment.getUser().getId().equals(msg.getUserId())) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        //답글 삭제
        deleteCommentRecursive(comment);

        // TODO: 소프트 삭제 로직은 추후 구현
        // comment.softDeleteRecursive();
        // commentRepository.save(comment);
    }

    @Transactional
    protected void deleteCommentRecursive(Comment comment) {
        // 자식 댓글 재귀 삭제
        for (Comment child : comment.getReplies()) {
            deleteCommentRecursive(child);
        }
        commentRepository.delete(comment);
    }

}
