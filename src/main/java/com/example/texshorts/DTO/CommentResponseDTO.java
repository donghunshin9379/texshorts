package com.example.texshorts.DTO;

import com.example.texshorts.entity.Comment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponseDTO {

    private Long id;

    private Long userId;
    private String nickname;

    private String content;

    private Integer likeCount;
    private Integer replyCount;

    private LocalDateTime createdAt;

    private Boolean isDeleted;

    private List<CommentResponseDTO> replies;  // 답글 목록 (optional)

    public CommentResponseDTO(Comment comment) {
        this.id = comment.getId();
        this.userId = comment.getUser().getId();
        this.nickname = comment.getUser().getNickname();
        this.content = comment.getIsDeleted() ? null : comment.getContent();
        this.likeCount = comment.getLikeCount();
        this.replyCount = comment.getReplyCount();
        this.createdAt = comment.getCreatedAt();
        this.isDeleted = comment.getIsDeleted();
    }

    public void setReplies(List<CommentResponseDTO> replies) {
        this.replies = replies;
    }

}

