package com.example.texshorts.repository;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    // 특정 유저의 특정 게시물 리액션 조회
    Optional<PostReaction> findByUserAndPost(User user, Post post);
    
    // 특정 게시물의 특정 타입(좋/실) 리액션 조회
    long countByPostIdAndType(Long postId, ReactionType type);

    // 특정 유저의 좋아요한 게시물
    @Query("SELECT pr.post FROM PostReaction pr WHERE pr.user.id = :userId AND pr.type = com.example.texshorts.entity.ReactionType.LIKE")
    List<Post> findLikedPostsByUserId(@Param("userId") Long userId);
}
