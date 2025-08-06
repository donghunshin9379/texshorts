package com.example.texshorts.service;

import com.example.texshorts.entity.TagActionType;
import com.example.texshorts.entity.ViewHistory;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        if (redisCacheService.hasViewed(userId, postId)) {
            logger.info("중복시청 if 필터 걸림! userId : {}", userId);
            logger.info("중복시청 if 필터 걸림! postId : {}", postId);
            return;
        }

        redisCacheService.incrementViewCount(postId);

        requestRedisQueue.enqueueViewHistorySave(userId, postId);

        redisCacheService.cacheViewHistory(userId, postId);

        requestRedisQueue.enqueueViewCountUpdate(postId);

    }


    /**
     * 여러 태그에 대해 관심태그 갱신 요청을 큐에 한꺼번에 등록
     */
    public void enqueueAddInterestTagsFromPost(Long userId, Long postId) {
        List<String> tagNames = postRepository.findTagNamesByPostId(postId); // 구체적 구현
        for (String tag : tagNames) {
            requestRedisQueue.enqueueUserInterestTagUpdate(userId, tag, TagActionType.ADD);
        }
    }

//    public void enqueueRemoveInterestTagsFromPost(Long userId, Long postId) {
//        List<String> tagNames = postRepository.findTagNamesByPostId(postId); // 구체적 구현
//        for (String tag : tagNames) {
//            requestRedisQueue.enqueueUserInterestTagUpdate(userId, tag, TagActionType.REMOVE);
//        }
//    }





}

