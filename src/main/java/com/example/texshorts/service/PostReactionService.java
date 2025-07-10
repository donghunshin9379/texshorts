package com.example.texshorts.service;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.PostReactionRepository;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public void react(Long postId, User user, ReactionType type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        // 기존 리액션 조회
        Optional<PostReaction> existing = postReactionRepository.findByUserAndPost(user, post);

        if (existing.isPresent()) {
            ReactionType previousType = existing.get().getType();

            // 같은 리액션이면 토글 해제 (취소)
            if (previousType == type) {
                postReactionRepository.delete(existing.get());
                decrementRedisCount(postId, type);
                return;
            }

            // 다른 리액션이면 교체
            postReactionRepository.delete(existing.get());
            decrementRedisCount(postId, previousType);
        }

        // 새 리액션 저장
        PostReaction newReaction = new PostReaction();
        newReaction.setUser(user);
        newReaction.setPost(post);
        newReaction.setType(type);
        postReactionRepository.save(newReaction);

        incrementRedisCount(postId, type);
    }

    private void incrementRedisCount(Long postId, ReactionType type) {
        String key = getRedisKey(postId, type);
        redisTemplate.opsForValue().increment(key);
    }

    private void decrementRedisCount(Long postId, ReactionType type) {
        String key = getRedisKey(postId, type);
        redisTemplate.opsForValue().decrement(key);
    }

    private String getRedisKey(Long postId, ReactionType type) {
        return "post:" + postId + ":" + type.name().toLowerCase();
    }
    
    // 좋아요 카운트 반환
    public Long getLikeCount(Long postId) {
        return getOrLoadReactionCount(postId, ReactionType.LIKE);
    }

    // Redis 연계 repository 요청
    private Long getOrLoadReactionCount(Long postId, ReactionType type) {
        String key = getRedisKey(postId, type);
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            return Long.parseLong(cached);
        }

        long count = postReactionRepository.countByPostIdAndType(postId, type);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
        return count;
    }


}
