package com.example.texshorts.service;

import com.example.texshorts.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentRepository commentRepository;

    private static final String COMMENT_COUNT_KEY_PREFIX = "post:commentCount:";

    public void incrementCommentCount(Long postId) {
        redisTemplate.opsForValue().increment(COMMENT_COUNT_KEY_PREFIX + postId);
    }

    public void decrementCommentCount(Long postId) {
        redisTemplate.opsForValue().decrement(COMMENT_COUNT_KEY_PREFIX + postId);
    }

    public int getCommentCountCached(Long postId) {
        String key = COMMENT_COUNT_KEY_PREFIX + postId;
        Integer cached = (Integer) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        int count = commentRepository.countByPostIdAndIsDeletedFalse(postId);
        redisTemplate.opsForValue().set(key, count);
        return count;
    }


    public void evictCommentList(Long postId) {
        redisTemplate.delete("post:comments:" + postId);
    }
}

