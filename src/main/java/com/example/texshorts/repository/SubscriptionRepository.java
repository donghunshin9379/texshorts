package com.example.texshorts.repository;

import com.example.texshorts.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Query("SELECT s.subscribed.id FROM Subscription s WHERE s.subscriber.id = :userId")
    List<Long> findSubscribedUserIdsByUserId(@Param("userId") Long userId);
}
