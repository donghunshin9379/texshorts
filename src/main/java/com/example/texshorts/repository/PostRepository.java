package com.example.texshorts.repository;

import com.example.texshorts.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = "user") // user를 페치조인으로 같이 가져오도록 설정
    Page<Post> findAll(Pageable pageable);
}