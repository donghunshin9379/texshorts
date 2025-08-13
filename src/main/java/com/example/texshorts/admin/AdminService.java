//package com.example.texshorts.admin;
//
//import com.example.texshorts.service.RedisCacheService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class AdminService {
//
//    private final RedisCacheService redisCacheService;
//
//    /**
//     * 게시물 피드 관련 Redis 캐시를 초기화
//     * - 최신, 인기, 개인화 피드 모두 삭제
//     */
//    public void clearPostFeedCache(Long userId) {
//        // 최신/인기 피드 전체 초기화
//        redisCacheService.clearCacheByPrefix(RedisCacheService.LATEST_POST_LIST_KEY_PREFIX);
//        redisCacheService.clearCacheByPrefix(RedisCacheService.POPULAR_POST_LIST_KEY_PREFIX);
//
//        // 개인화 피드는 userId 필요
//        if (userId != null) {
//            String personalizedPrefix = RedisCacheService.PERSONALIZED_POST_LIST_KEY_PREFIX + userId + ":";
//            redisCacheService.clearCacheByPrefix(personalizedPrefix);
//        }
//    }
//}
