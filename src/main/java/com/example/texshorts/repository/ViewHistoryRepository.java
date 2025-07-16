package com.example.texshorts.repository;

import com.example.texshorts.entity.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
