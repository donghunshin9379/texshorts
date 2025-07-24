package com.example.texshorts.repository;

import com.example.texshorts.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 포스트 조회수 */
public interface PostViewRepository extends JpaRepository<PostView, Long> {

    Optional<PostView> findByPostId(Long postId);

}