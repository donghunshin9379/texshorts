package com.example.texshorts.repository;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 모든 게시물 Page 단위 조회
     * User 엔티티 join 한 번에 조회
     */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Post> findAll(Pageable pageable);

    /**
     * 관심 태그 기반 게시물 조회
     */
    @Query("""
        SELECT pt.post
        FROM PostTag pt
        JOIN pt.tagHub th
        WHERE th.tagName IN :tagNames
        GROUP BY pt.post.id
        ORDER BY pt.post.createdAt DESC
    """)
    List<Post> findDistinctPostsByTagNames(List<String> tagNames, Pageable pageable);

    /**
     * 최신 게시물 조회
     */
    @Query("""
        SELECT p
        FROM Post p
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findDistinctLatestPosts(Pageable pageable);

    /**
     * 인기 게시물 조회
     */
    @Query("""
        SELECT p
        FROM Post p
        ORDER BY p.viewCount DESC, p.likeCount DESC, p.commentCount DESC
    """)
    Page<Post> findDistinctPopularPosts(Pageable pageable);



    // Post 테이블 > PostTag > TagHub > tagname 추출
    @Query("SELECT th.tagName FROM Post p " +
            "JOIN p.postTags pt " +
            "JOIN pt.tagHub th " +
            "WHERE p.id = :postId")
    List<String> findTagNamesByPostId(@Param("postId") Long postId);


    // Post 테이블 comment_count 증가
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = :count WHERE p.id = :postId")
    void updateCommentCount(@Param("postId") Long postId, @Param("count") int count);

    // Post 테이블 view_count 증가
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = :count WHERE p.id = :postId")
    void updateViewCount(@Param("postId") Long postId, @Param("count") int count);

    // 유저 ID로 게시물 조회, 페이징 적용
    Page<Post> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT p.viewCount FROM Post p WHERE p.id = :postId")
    Long getViewCountByPostId(@Param("postId") Long postId);


}
