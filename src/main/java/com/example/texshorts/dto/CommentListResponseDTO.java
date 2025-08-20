package com.example.texshorts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentListResponseDTO {
    private List<CommentResponseDTO> comments;
    private Long lastCommentId;

    // 리스트만 받아서 lastCommentId 자동 계산
    public CommentListResponseDTO(List<CommentResponseDTO> comments) {
        this.comments = comments;
        this.lastCommentId = comments.isEmpty() ? null : comments.get(comments.size() - 1).getId();
    }
}

