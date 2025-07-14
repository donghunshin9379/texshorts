package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "thumbnail_path", nullable = false)
    private String thumbnailPath;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    private String tags;

    private String location;

    @Column(nullable = false)
    private String visibility; // 예: "전체 공개", "구독자만", "비공개"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 양방향 관계
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostReaction> reactions = new ArrayList<>();
}