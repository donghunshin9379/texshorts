package com.example.texshorts.service;

import com.example.texshorts.entity.ViewHistory;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ViewService {

    private final ViewHistoryRepository viewHistoryRepository;
    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final RequestRedisQueue requestRedisQueue;
    private static final Logger logger = LoggerFactory.getLogger(ViewService.class);

    @Transactional
    public void increaseViewCountIfNotViewed(Long postId, Long userId) {

        /** 중복시청 확인 */
        if (redisCacheService.hasViewed(userId, postId)) { /**캐시에 조회기록 있을시 실행 X*/
            logger.info("중복시청 if 필터 걸림! userId : {}", userId);
            logger.info("중복시청 if 필터 걸림! postId : {}", postId);
            return;
        }
        /** 게시물 조회수 증가 (캐시) */
        redisCacheService.incrementViewCount(postId);

        /** 유저 조회 기록 저장 (DB) #태그 추출용# */
        viewHistoryRepository.save(
                new ViewHistory(userId, postRepository.getReferenceById(postId), LocalDateTime.now())
        );

        /** 유저 기록 저장 (캐시) */
        redisCacheService.cacheViewHistory(userId, postId);


        requestRedisQueue.enqueueViewCountUpdate(postId);

    }



}

