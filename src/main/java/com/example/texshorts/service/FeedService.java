//package com.example.texshorts.service;
//
//import com.example.texshorts.DTO.PostResponseDTO;
//import com.example.texshorts.entity.Post;
//import com.example.texshorts.repository.PostReactionRepository;
//import com.example.texshorts.repository.PostRepository;
//import com.example.texshorts.repository.SubscriptionRepository;
//import com.example.texshorts.repository.ViewHistoryRepository;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//
//public class FeedService {
//    private final PostReactionRepository postReactionRepository;
//    private final ViewHistoryRepository viewHistoryRepository;
//    private final PostRepository postRepository;
//    private final SubscriptionRepository subscriptionRepository;
//    private final RedisCacheService redisCacheService;
//    private final ObjectMapper objectMapper;
//
//    @Value("${feed.redis.ttl.minutes}")
//    private long ttlMinutes;
//
//    // 홈탭 피드
//    public List<PostResponseDTO> generateHomeFeed(Long userId, int page, int size) {
//        String cacheKey = "homeFeed::user:" + userId + "::page:" + page + "::size:" + size;
//        String cached = redisCacheService.get(cacheKey);
//
//        if (cached != null) {
//            try {
//                return objectMapper.readValue(cached, new TypeReference<List<PostResponseDTO>>() {});
//            } catch (Exception e) {
//                System.err.println("캐시 역직렬화 실패: " + e.getMessage());
//            }
//        }
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//        // 1. 좋아요 누른 게시물 태그
//        List<Post> likedPosts = postReactionRepository.findLikedPostsByUserId(userId);
//        Set<String> likedTagNames = likedPosts.stream()
//                .flatMap(p -> p.getTags().stream())
//                .map(Tag::getTagName)
//                .collect(Collectors.toSet());
//
//        // 2. 조회한 게시물 태그
//        List<Post> viewedPosts = viewHistoryRepository.findViewedPostsByUserId(userId);
//        Set<String> viewedTagNames = viewedPosts.stream()
//                .flatMap(p -> p.getTags().stream())
//                .map(Tag::getTagName)
//                .collect(Collectors.toSet());
//
//        // 3. 구독한 유저 ID
//        List<Long> subscribedUserIds = subscriptionRepository.findSubscribedUserIdsByUserId(userId);
//
//        // 4. 구독 유저의 게시물
//        Page<Post> subscribedUserPosts = postRepository.findByUser_IdIn(subscribedUserIds, pageable);
//
//        // 5. 태그 기반 추천
//        Set<String> combinedTagNames = new HashSet<>();
//        combinedTagNames.addAll(likedTagNames);
//        combinedTagNames.addAll(viewedTagNames);
//
//        List<Post> tagBasedPosts = postRepository
//                .findDistinctByTags_TagNameInIgnoreCase(new ArrayList<>(combinedTagNames), pageable)
//                .stream()
//                .filter(post -> !subscribedUserIds.contains(post.getUserId()))
//                .collect(Collectors.toList());
//
//        // 6. 합치기 및 정렬
//        List<Post> finalFeed = new ArrayList<>();
//        finalFeed.addAll(subscribedUserPosts.getContent());
//        finalFeed.addAll(tagBasedPosts);
//
//        finalFeed = finalFeed.stream()
//                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
//                .skip(page * size)
//                .limit(size)
//                .collect(Collectors.toList());
//
//        List<PostResponseDTO> responseDTOs = finalFeed.stream()
//                .map(PostResponseDTO::new)
//                .toList();
//
//        try {
//            String json = objectMapper.writeValueAsString(responseDTOs);
//            redisCacheService.set(cacheKey, json, Duration.ofMinutes(ttlMinutes));
//        } catch (Exception e) {
//            System.err.println("캐시 직렬화 실패: " + e.getMessage());
//        }
//
//        return responseDTOs;
//    }
//}