package com.example.texshorts.dto;

import com.example.texshorts.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


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
    private Long postId;

    private int viewCount;
    private int likeCount;
    private long dislikeCount;
    private int commentCount;
    private int replyCount;
    private boolean hasLiked;
    private boolean hasDisliked;

    private List<Long> rootCommentIds;
    private Map<Long, List<Long>> replyIds;

    /**
     * 썸네일 없이 DTO 변환 (마이페이지 전용 등)
     */
    public static PostResponseDTO fromEntityWithoutThumbnail(Post post) {
        PostResponseDTO dto = new PostResponseDTO();

        dto.setNickname(post.getUser().getNickname());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setThumbnailUrl(null); // 썸네일 제외
        dto.setCreatedAt(post.getCreatedAt());
        dto.setTags(post.getTags());
        dto.setPostId(post.getId());

        if(post.getContent() != null) {
            dto.setContentLines(List.of(post.getContent().split("\n")));
        }

        return dto;
    }
}