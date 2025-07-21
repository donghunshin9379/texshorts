package com.example.texshorts.DTO;

import com.example.texshorts.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final String tags;

    public PostResponseDTO(Post post) {
        this.nickname = post.getUser().getNickname(); // User 닉네임
        this.title = post.getTitle();
        this.content = post.getContent();
        this.thumbnailPath = post.getThumbnailPath();
        this.createdAt = post.getCreatedAt();
        this.contentLines = contentToLines(post.getContent());
        this.tags = post.getTags();
    }

    private List<String> contentToLines(String content) {
        if (content == null || content.isEmpty()) return List.of();

        List<String> rawLines = Arrays.stream(
                        content.replaceAll("\\r\\n", "\n")     //CRLF → LF
                                .replaceAll("\n{3,}", "\n\n")   //줄바꿈 3번 이상 → 2번
                                .split("\n")
                )
                .map(String::trim)
                .collect(Collectors.toList());

        return filterConsecutiveEmptyLines(rawLines);
    }

    /**
     * @param lines 원본 줄 리스트
     * @return 필터링된 줄 리스트
     */
    //실제 contentLines(본문) 반환
    private List<String> filterConsecutiveEmptyLines(List<String> lines) {
        List<String> filtered = new ArrayList<>();
        boolean lastLineWasEmpty = false;

        for (String line : lines) {
            if (line.isEmpty()) {
                if (!lastLineWasEmpty) {
                    filtered.add(line);  // 빈 줄 한 번만 허용
                    lastLineWasEmpty = true;
                }
                // 연속 빈 줄은 추가하지 않음
            } else {
                filtered.add(line);
                lastLineWasEmpty = false;
            }
        }
        return filtered;
    }




}