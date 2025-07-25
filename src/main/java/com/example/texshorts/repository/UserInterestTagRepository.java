package com.example.texshorts.repository;

import com.example.texshorts.entity.UserInterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInterestTagRepository extends JpaRepository<UserInterestTag, Long> {

    List<UserInterestTag> findByUserId(Long userId);

    boolean existsByUserIdAndTagHubId(Long userId, Long tagHubId);

    void deleteByUserIdAndTagHubId(Long userId, Long tagHubId);

}

