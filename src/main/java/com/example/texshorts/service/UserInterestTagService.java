package com.example.texshorts.service;

import com.example.texshorts.entity.TagActionType;
import com.example.texshorts.entity.TagHub;
import com.example.texshorts.entity.User;
import com.example.texshorts.entity.UserInterestTag;
import com.example.texshorts.repository.PostRepository;
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








}