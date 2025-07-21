package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // 삭제된 post 염두 nullable = true
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;

    private LocalDateTime viewedAt;


    // 편의 생성자
    public ViewHistory(Long userId, Post post, LocalDateTime viewedAt) {
        this.userId = userId;
        this.post = post;
        this.viewedAt = viewedAt;
    }


}

