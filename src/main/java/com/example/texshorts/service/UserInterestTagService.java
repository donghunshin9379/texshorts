package com.example.texshorts.service;

import com.example.texshorts.entity.TagHub;
import com.example.texshorts.entity.User;
import com.example.texshorts.entity.UserInterestTag;
import com.example.texshorts.repository.TagHubRepository;
import com.example.texshorts.repository.UserInterestTagRepository;
import com.example.texshorts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInterestTagService {

    private final UserInterestTagRepository userInterestTagRepository;
    private final TagHubRepository tagHubRepository;
    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;


    // 관심 태그 조회
    public List<String> getUserInterestTags(Long userId) {
        // Supplier로 DB 직접 조회 로직 전달
        Set<String> tagsSet = redisCacheService.getUserInterestTags(userId, () ->
                userInterestTagRepository.findByUserId(userId).stream()
                        .map(uit -> uit.getTagHub().getTagName())
                        .collect(Collectors.toSet())
        );

        return tagsSet == null ? List.of() : List.copyOf(tagsSet);
    }


    // 관심 태그 추가
    @Transactional
    public void addUserInterestTag(Long userId, String tagName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 태그허브 존재 여부
        TagHub tagHub = tagHubRepository.findByTagName(tagName)
                .orElseThrow(() -> new RuntimeException("TagHub not found")); 

        // 중복 체크
        boolean exists = userInterestTagRepository.existsByUserIdAndTagHubId(userId, tagHub.getId());
        if (exists) return;

        UserInterestTag userInterestTag = UserInterestTag.builder()
                .user(user)
                .tagHub(tagHub)
                .build();
        userInterestTagRepository.save(userInterestTag);

        // 캐시에 단일 태그 추가
        redisCacheService.addUserInterestTagToCache(userId, tagName);
    }


    // 관심 태그 삭제
    @Transactional
    public void removeUserInterestTag(Long userId, String tagName) {
        // 존재하는 태그만 삭제 시도
        tagHubRepository.findByTagName(tagName).ifPresent(tagHub -> {
            userInterestTagRepository.deleteByUserIdAndTagHubId(userId, tagHub.getId());

            // 캐시에서 단일 태그 제거
            redisCacheService.removeUserInterestTagFromCache(userId, tagName);
        });
    }





}