package com.example.texshorts.dto.message;

import com.example.texshorts.entity.TagActionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestTagQueueMessage {
    private Long userId;
    private String tagName;
    private TagActionType action;
}
