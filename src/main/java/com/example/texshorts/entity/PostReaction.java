package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PostReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //JPA 객체 필드이름( 외래키임으로 user_id )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //JPA 객체 필드이름( 외래키임으로 post_id )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING) //@Enumerated 필드명 그대로
    private ReactionType type; // LIKE, DISLIKE
}

