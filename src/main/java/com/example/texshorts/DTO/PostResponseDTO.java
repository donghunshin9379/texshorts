package com.example.texshorts.DTO;

import com.example.texshorts.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class PostResponseDTO {
    private final String nickname;
    private final String title;
    private final String content;
    private final String thumbnailPath;
    private final LocalDateTime createdAt;
    private final List<String> contentLines;

    public PostResponseDTO(Post post) {
        this.nickname = post.getUser().getNickname(); //User 닉네임
        this.title = post.getTitle();
        this.content = post.getContent();
        this.thumbnailPath = post.getThumbnailPath();
        this.createdAt = post.getCreatedAt();
        this.contentLines = Arrays.stream(
                        post.getContent()
                                .replaceAll("\\r\\n", "\n")
                                .replaceAll("\n{3,}", "\n\n")  // 연속 줄바꿈 3개 이상 -> 2개로 제한
                                .split("\n")
                ).map(String::trim)  // 앞뒤 공백 제거
                .filter(line -> !line.isEmpty() || /* 빈 줄은 한 번만 허용하는 로직  */ true)
                .collect(Collectors.toList());

    }
}
