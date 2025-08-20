package com.example.texshorts.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDeleteMessage {
    private Long commentId;
    private Long userId;
}
