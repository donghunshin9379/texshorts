package com.example.texshorts.DTO;

import com.example.texshorts.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class PostResponseDTO {
    private final String nickname;
    private final String title;
    private final String content;
    private final String thumbnailPath;
    private final LocalDateTime createdAt;

    public PostResponseDTO(Post post) {
        this.nickname = post.getUser().getNickname(); //User의 닉네임
        this.title = post.getTitle();
        this.content = post.getContent();
        this.thumbnailPath = post.getThumbnailPath();
        this.createdAt = post.getCreatedAt();
    }
}
