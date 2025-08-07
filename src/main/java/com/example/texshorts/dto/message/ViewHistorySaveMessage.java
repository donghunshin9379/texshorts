package com.example.texshorts.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ViewHistorySaveMessage {
    private Long userId;
    private Long postId;
}
