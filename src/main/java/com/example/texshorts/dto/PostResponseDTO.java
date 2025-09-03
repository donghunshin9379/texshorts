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

    private int likeCount;
    private int commentCount;
    private int viewCount;
    private int replyCount;

    // 댓글 ID 리스트 (Lazy load 가능)
    private List<Long> rootCommentIds;

    // 답글 ID 매핑: parentCommentId -> 답글 ID 리스트
    private Map<Long, List<Long>> replyIds;

    // 유저별 좋아요/싫어요 상태 (캐시 기반)
    private Map<Long, Boolean> userLikedMap;
    private Map<Long, Boolean> userDislikedMap;


    /**
     * Entity → DTO 변환 메서드
     * @param post 게시물 엔티티
     * @param urlGenerator 썸네일 파일명을 URL로 변환하는 함수
     */
//    public static PostResponseDTO fromEntity(Post post, Function<String, String> urlGenerator) {
//        PostResponseDTO dto = new PostResponseDTO();
//
//        dto.setNickname(post.getUser().getNickname());
//        dto.setTitle(post.getTitle());
//        dto.setContent(post.getContent());
//        dto.setThumbnailUrl(urlGenerator.apply(post.getThumbnailPath()));
//        dto.setCreatedAt(post.getCreatedAt());
//        dto.setTags(post.getTags());
//        dto.setPostId(post.getId());
//
//        // contentLines가 있다면, 예를 들어 content를 줄 단위로 나누기
//        if(post.getContent() != null) {
//            dto.setContentLines(List.of(post.getContent().split("\n")));
//        }
//
//        return dto;
//    }


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