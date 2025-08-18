package com.example.texshorts.dto.message;

import com.example.texshorts.dto.PostCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreationMessage {
    private String thumbnailPath;
    private PostCreateRequest postCreateRequest;
    private Long userId;
}
