package com.example.texshorts.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyCommentCreationMessage {
    private Long parentCommentId;
    private Long userId;
    private String content;
}
