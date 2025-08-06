package com.example.texshorts.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ViewHistorySaveMessage {
    private Long userId;
    private Long postId;
}
