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

    private Long postId;

    private LocalDateTime viewedAt;


    // 편의 생성자
    public ViewHistory(Long userId, Long postId, LocalDateTime viewedAt) {
        this.userId = userId;
        this.postId = postId;
        this.viewedAt = viewedAt;
    }

}

