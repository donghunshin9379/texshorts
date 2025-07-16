package com.example.texshorts.repository;

import com.example.texshorts.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdAndParentIsNullAndIsDeletedFalse(Long postId);

}
