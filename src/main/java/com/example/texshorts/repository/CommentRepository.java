package com.example.texshorts.repository;

import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.entity.Comment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 루트 댓글 수 조회
    int countByPostIdAndParentIsNullAndIsDeletedFalse(Long postId);

    // 댓글 답글 수 조회
    int countByParentIdAndIsDeletedFalse(Long parentId);


    /** 삭제 */
    // 페이징쿼리
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    List<Comment> findTopNByPostId(@Param("postId") Long postId, Pageable pageable);

    // n개 조회
    default List<Comment> findTopNByPostId(Long postId, int n) {
        return findTopNByPostId(postId, PageRequest.of(0, n));
    }

    /**
     * JPQL 기반 DTO생성 쿼리 */
    @Query("SELECT new com.example.texshorts.dto.CommentResponseDTO(" +
            "c.id, u.id, u.nickname, " +
            "CASE WHEN c.isDeleted = true THEN null ELSE c.content END, " +
            "c.likeCount, c.replyCount, c.createdAt, c.isDeleted, null) " +
            "FROM Comment c JOIN c.user u " +
            "WHERE c.post.id = :postId AND c.parent IS NULL AND c.isDeleted = false")
    List<CommentResponseDTO> findRootCommentDTOs(@Param("postId") Long postId);

    @Query("SELECT new com.example.texshorts.dto.CommentResponseDTO(" +
            "c.id, u.id, u.nickname, " +
            "CASE WHEN c.isDeleted = true THEN null ELSE c.content END, " +
            "c.likeCount, c.replyCount, c.createdAt, c.isDeleted, null) " +
            "FROM Comment c JOIN c.user u " +
            "WHERE c.parent.id = :parentCommentId")
    List<CommentResponseDTO> findReplyDTOs(@Param("parentCommentId") Long parentCommentId);



}
