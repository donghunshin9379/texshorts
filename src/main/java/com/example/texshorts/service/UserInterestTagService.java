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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInterestTagService {

    private final UserInterestTagRepository userInterestTagRepository;
    private final TagHubRepository tagHubRepository;
    private final UserRepository userRepository;

    // 관심 태그 조회
    public List<String> getUserInterestTags(Long userId) {
        return userInterestTagRepository.findByUserId(userId).stream()
                .map(uit -> uit.getTagHub().getTagName())
                .collect(Collectors.toList());
    }

    // 관심 태그 추가
    @Transactional
    public void addUserInterestTag(Long userId, String tagName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TagHub tagHub = tagHubRepository.findByTagName(tagName)
                .orElseGet(() -> tagHubRepository.save(TagHub.builder().tagName(tagName).usageCount(0L).build()));

        /** */
        if (!userInterestTagRepository.existsByUserIdAndTagHubId(userId, tagHub.getId())) {
            UserInterestTag userInterestTag = UserInterestTag.builder()
                    .user(user)
                    .tagHub(tagHub)
                    .build();
            userInterestTagRepository.save(userInterestTag);
        }
    }

    // 관심 태그 삭제
    @Transactional
    public void removeUserInterestTag(Long userId, String tagName) {
        TagHub tagHub = tagHubRepository.findByTagName(tagName)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        userInterestTagRepository.deleteByUserIdAndTagHubId(userId, tagHub.getId());

        tagHub.decrementUsageCount();
        tagHubRepository.save(tagHub);
    }


}