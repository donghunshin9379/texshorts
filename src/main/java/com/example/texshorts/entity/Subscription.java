package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "subscribed_id"}))
@Getter
@Setter //수정/임시저장 O, 불변 X.
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구독한 사람 (내가 구독한.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private User subscriber;

    // 구독 대상 (나한테 구독당한.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscribed_id", nullable = false)
    private User subscribed;

    private LocalDateTime subscribedAt = LocalDateTime.now();
}
