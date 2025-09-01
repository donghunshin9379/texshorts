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
    /** 루트 댓글 (마지막이 최신댓글)*/
    @Query("SELECT new com.example.texshorts.dto.CommentResponseDTO(" +
            "c.id, u.id, u.nickname, " +
            "CASE WHEN c.isDeleted = true THEN null ELSE c.content END, " +
            "c.likeCount, c.replyCount, c.createdAt, c.isDeleted, null) " +
            "FROM Comment c JOIN c.user u " +
            "WHERE c.post.id = :postId AND c.parent IS NULL AND c.isDeleted = false " +
            "ORDER BY c.id ASC")
    List<CommentResponseDTO> findRootCommentDTOs(@Param("postId") Long postId);

    /** 답글 (마지막이 최신답글)*/
    @Query("SELECT new com.example.texshorts.dto.CommentResponseDTO(" +
            "c.id, u.id, u.nickname, " +
            "CASE WHEN c.isDeleted = true THEN null ELSE c.content END, " +
            "c.likeCount, c.replyCount, c.createdAt, c.isDeleted, null) " +
            "FROM Comment c JOIN c.user u " +
            "WHERE c.parent.id = :parentCommentId ORDER BY c.id ASC")
    List<CommentResponseDTO> findReplyDTOs(@Param("parentCommentId") Long parentCommentId);

    /**마지막으로 본 댓글 ID 이후의 댓글 */
    @Query("""
    SELECT new com.example.texshorts.dto.CommentResponseDTO(
        c.id, c.content, c.user.id, c.user.nickname, c.createdAt
    )
    FROM Comment c
    WHERE c.post.id = :postId
      AND c.parent IS NULL
      AND c.id > :lastCommentId
    ORDER BY c.id ASC
    """)
    List<CommentResponseDTO> findRootCommentsAfter(@Param("postId") Long postId,
                                                   @Param("lastCommentId") Long lastCommentId);

    /**마지막으로 본 답글 ID 이후의 댓글 */
    @Query("SELECT new com.example.texshorts.dto.CommentResponseDTO(c.id, c.content, c.user.id, c.createdAt) " +
            "FROM Comment c " +
            "WHERE c.parent.id = :parentCommentId " +
            "AND c.id > :lastReplyId " +
            "ORDER BY c.id ASC")
    List<CommentResponseDTO> findRepliesAfter(@Param("parentCommentId") Long parentCommentId,
                                              @Param("lastReplyId") Long lastReplyId);


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





}
