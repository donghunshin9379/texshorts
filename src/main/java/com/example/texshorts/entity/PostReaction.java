package com.example.texshorts.entity;

import jakarta.persistence.*;

@Entity
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @Enumerated(EnumType.STRING)
    private ReactionType type; // LIKE, DISLIKE
}

