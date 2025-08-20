package com.example.texshorts.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostDeleteMessage {
    private Long postId;
    private Long userId;
}
