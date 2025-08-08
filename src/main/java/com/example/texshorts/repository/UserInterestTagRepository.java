package com.example.texshorts.repository;

import com.example.texshorts.entity.UserInterestTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInterestTagRepository extends JpaRepository<UserInterestTag, Long> {

    List<UserInterestTag> findByUserId(Long userId);

    boolean existsByUserIdAndTagHubId(Long userId, Long tagHubId);

    void deleteByUserIdAndTagHubId(Long userId, Long tagHubId);

    // 유저 관심태그명만 리스트로 조회
    @Query("SELECT t.tagHub.tagName FROM UserInterestTag t WHERE t.user.id = :userId")
    List<String> findTagsByUserId(@Param("userId") Long userId);

}

