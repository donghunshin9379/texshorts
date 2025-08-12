package com.example.texshorts.service;

import com.example.texshorts.entity.TagActionType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestTagQueueService {

    private final RedisCacheService redisCacheService;
    private final RequestRedisQueue requestRedisQueue;
    private static final Logger logger = LoggerFactory.getLogger(InterestTagQueueService.class);

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
}
