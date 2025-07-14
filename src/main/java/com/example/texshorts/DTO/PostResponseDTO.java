package com.example.texshorts.DTO;

import com.example.texshorts.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class PostResponseDTO { //포스트(게시물) 객체
    private final String nickname;
    private final String title;
    private final String content;
    private final String thumbnailPath;
    private final LocalDateTime createdAt;
    private final List<String> contentLines;
    private final List<String> tags;

    public PostResponseDTO(Post post) {
        this.nickname = post.getUser().getNickname(); // User 닉네임
        this.title = post.getTitle();
        this.content = post.getContent();
        this.thumbnailPath = post.getThumbnailPath();
        this.createdAt = post.getCreatedAt();
        this.contentLines = contentToLines(post.getContent());
        this.tags = parseTags(post.getTags());
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


    //태그 문자열을 쉼표(,) 기준으로 파싱
    //@param tagsStr "tag1, tag2, tag3" 형식의 태그 문자열
    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isEmpty()) {
            return List.of();
        }
        //@return 태그 리스트 (null일 경우 빈 리스트 반환)
        return Arrays.stream(tagsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

}