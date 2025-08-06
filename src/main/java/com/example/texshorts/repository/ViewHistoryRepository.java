package com.example.texshorts.repository;

import com.example.texshorts.entity.ViewHistory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
/** 유저의 시청기록 */
public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    /** 시청 기록 조회*/
    // n개씩 조회
    default List<ViewHistory> findTopNByPostId(Long postId, int n) {
        return findTopNByPostId(postId, PageRequest.of(0, n));
    }
    //페이징쿼리
    @Query("SELECT v FROM ViewHistory v WHERE v.post.id = :postId")
    List<ViewHistory> findTopNByPostId(@Param("postId") Long postId, Pageable pageable);

    // 중복 기록 방지
    boolean existsByUserIdAndPostId(Long userId, Long postId);


}
