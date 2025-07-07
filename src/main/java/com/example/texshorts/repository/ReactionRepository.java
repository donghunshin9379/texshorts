package com.example.texshorts.repository;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReactionRepository extends JpaRepository<PostReaction, Long> {

    long countByPostAndType(Post post, ReactionType type);

    // 사용자가 좋아요 누른 게시물 조회
    List<PostReaction> findByUserAndType(User user, ReactionType type);
}
