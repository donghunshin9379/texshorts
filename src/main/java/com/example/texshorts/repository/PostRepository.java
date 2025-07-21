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

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 모든 게시물 Page 단위만큼
    @Override
    @EntityGraph(attributePaths = "user") // user를 페치조인으로 같이 가져오도록 설정
    Page<Post> findAll(Pageable pageable);

    // 게시물 조회수 증가 쿼리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    // 특정 유저가 구독한 유저(작성자) 게시물 select





}