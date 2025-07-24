package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "post_views")
@Data
public class PostView { /** 게시물의 조회수 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId; // Post의 id와 동일

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

}
