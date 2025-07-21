package com.example.texshorts.repository;

import com.example.texshorts.entity.Comment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdAndParentIsNullAndIsDeletedFalse(Long postId);

    // 페이징쿼리
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    List<Comment> findTopNByPostId(@Param("postId") Long postId, Pageable pageable);

    // n개 조회
    default List<Comment> findTopNByPostId(Long postId, int n) {
        return findTopNByPostId(postId, PageRequest.of(0, n));
    }

    int countByPostIdAndIsDeletedFalse(Long postId);



}
