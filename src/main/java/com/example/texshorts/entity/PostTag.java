package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_hub_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_hub_id", nullable = false)
    private TagHub tagHub;
}
