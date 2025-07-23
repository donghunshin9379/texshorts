package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CommentReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING) //@Enumerated 필드명 그대로
    private ReactionType type; // LIKE, DISLIKE
}
