package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_interest_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tag_hub_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterestTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_hub_id", nullable = false)
    private TagHub tagHub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}