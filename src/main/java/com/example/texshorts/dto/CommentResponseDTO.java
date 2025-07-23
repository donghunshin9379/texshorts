package com.example.texshorts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
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


    public void setReplies(List<CommentResponseDTO> replies) {
        this.replies = replies;
    }





}

