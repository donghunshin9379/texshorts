package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "posts")
@Getter
@Setter //수정/임시저장 O, 불변 X.
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;



    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "thumbnail_path", nullable = false)
    private String thumbnailPath;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    private String location;

    @Column(nullable = false)
    private String visibility; // 예: "전체 공개", "구독자만", "비공개"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 단순 텍스트 저장(보여지는 태그)
    @Column(name = "tags")
    private String tags;

    //좋아요 갯수
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    // 댓글 갯수
    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    // 실제 태그 저장
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();
}