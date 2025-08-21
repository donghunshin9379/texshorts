package com.example.texshorts.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private Long userId;
    private String nickname;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
    private List<CommentResponseDTO> replies;

    // 기존 9인자 생성자 (루트 댓글, 답글 리스트용)
    public CommentResponseDTO(Long id, Long userId, String nickname, String content, Integer likeCount, Integer replyCount,
                              LocalDateTime createdAt, Boolean isDeleted, Object unusedNull) {
        this.id = id;
        this.userId = userId;
        this.nickname = nickname;
        this.content = content;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
        this.replies = new ArrayList<>();
    }

    // 새로고침용 루트 댓글 조회 쿼리용 생성자 (5인자)
    public CommentResponseDTO(Long id, String content, Long userId, String nickname, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.replies = new ArrayList<>();
    }

    // 새로고침용 답글 조회 쿼리용 생성자 (4인자)
    public CommentResponseDTO(Long id, String content, Long userId, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.createdAt = createdAt;
        this.replies = new ArrayList<>();
    }

    public void setReplies(List<CommentResponseDTO> replies) {
        this.replies = replies;
    }



}

