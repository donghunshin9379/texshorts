package com.example.texshorts.repository;

import com.example.texshorts.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;


@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 모든 게시물 Page 단위만큼
    @Override
    @EntityGraph(attributePaths = "user") // user를 페치조인으로 같이 가져오도록 설정
    Page<Post> findAll(Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = :count WHERE p.id = :postId")
    void updateCommentCount(@Param("postId") Long postId, @Param("count") int count);


    @Modifying
    @Query("UPDATE Post p SET p.viewCount = :count WHERE p.id = :postId")
    void updateViewCount(@Param("postId") Long postId, @Param("count") int count);






}