package com.example.texshorts.service;

import com.example.texshorts.entity.TagActionType;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ViewService {

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
     * AOP 호출용
     * 관심태그 추가 갱신 큐
     */
    public void enqueueAddInterestTagsFromPost(Long userId, Long postId) {
        logger.info("AOP > ViewService 호출완료");

        List<String> tagNames = redisCacheService.getTagNamesByPostId(postId);
        for (String tag : tagNames) {
            requestRedisQueue.enqueueUserInterestTagUpdate(userId, tag, TagActionType.ADD);
        }
    }

    /**
     * AOP 호출용
     * 관심태그 삭제 갱신 큐
     */
    public void enqueueRemoveInterestTagsFromPost(Long userId, Long postId) {
        logger.info("AOP > 관심태그 삭제 요청");
        List<String> tagNames = redisCacheService.getTagNamesByPostId(postId);
        for (String tag : tagNames) {
            requestRedisQueue.enqueueUserInterestTagUpdate(userId, tag, TagActionType.REMOVE);
        }
    }


//    public void enqueueRemoveInterestTagsFromPost(Long userId, Long postId) {
//        List<String> tagNames = postRepository.findTagNamesByPostId(postId); // 구체적 구현
//        for (String tag : tagNames) {
//            requestRedisQueue.enqueueUserInterestTagUpdate(userId, tag, TagActionType.REMOVE);
//        }
//    }





}

