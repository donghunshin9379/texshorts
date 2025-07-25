package com.example.texshorts.dto;

import com.example.texshorts.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDTO {  /** 게시물 출력용 바디 DTO*/
    private  String nickname;
    private  String title;
    private String content;
    private  String thumbnailUrl;
    private LocalDateTime createdAt;
    private List<String> contentLines;
    private String tags;

    /**
     * Entity → DTO 변환 메서드
     * @param post 게시물 엔티티
     * @param urlGenerator 썸네일 파일명을 URL로 변환하는 함수
     */
    public static PostResponseDTO fromEntity(Post post, Function<String, String> urlGenerator) {
        PostResponseDTO dto = new PostResponseDTO();

        dto.setNickname(post.getUser().getNickname());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setThumbnailUrl(urlGenerator.apply(post.getThumbnailPath()));  // 썸네일 경로 → URL
        dto.setCreatedAt(post.getCreatedAt());
        dto.setTags(post.getTags());

        // contentLines가 있다면, 예를 들어 content를 줄 단위로 나누기
        if(post.getContent() != null) {
            dto.setContentLines(List.of(post.getContent().split("\n")));
        }

        return dto;
    }
}