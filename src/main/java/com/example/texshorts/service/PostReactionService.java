package com.example.texshorts.service;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.PostReactionRepository;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final RedisCacheService redisCacheService;

    private String getRedisKey(Long postId, ReactionType type) {
        return "post:" + postId + ":" + type.name().toLowerCase();
    }

    public void react(Long postId, User user, ReactionType type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        Optional<PostReaction> existing = postReactionRepository.findByUserAndPost(user, post);

        if (existing.isPresent()) {
            ReactionType previousType = existing.get().getType();

            if (previousType == type) {
                deleteReaction(existing.get(), postId, type);
                return;
            }
            deleteReaction(existing.get(), postId, previousType);
        }
        saveNewReaction(post, user, type, postId);
    }

    // 기존 리액션 삭제
    private void deleteReaction(PostReaction reaction, Long postId, ReactionType type) {
        postReactionRepository.delete(reaction);
        redisCacheService.decrement(getRedisKey(postId, type));
        redisCacheService.delete(getRedisKey(postId, type));
    }

    // 첫리액션 or 기존 리액션 변경
    private void saveNewReaction(Post post, User user, ReactionType type, Long postId) {
        PostReaction newReaction = new PostReaction();
        newReaction.setPost(post);
        newReaction.setUser(user);
        newReaction.setType(type);
        postReactionRepository.save(newReaction);

        redisCacheService.increment(getRedisKey(postId, type));
        redisCacheService.delete(getRedisKey(postId, type));
    }

    // 좋아요 카운트 반환
    public Long getLikeCount(Long postId) {
        String key = getRedisKey(postId, ReactionType.LIKE);
        Long cachedCount = null;

        try {
            String cached = redisCacheService.get(key);
            if (cached != null) {
                cachedCount = Long.parseLong(cached);
                return cachedCount;
            }
        } catch (Exception e) {
            System.err.println("Redis 캐시 조회 실패, DB에서 조회");
        }

        long count = postReactionRepository.countByPostIdAndType(postId, ReactionType.LIKE);

        try {
            redisCacheService.set(key, String.valueOf(count), Duration.ofMinutes(10)); // TTL 10분
        } catch (Exception e) {
            System.err.println("Redis 캐시 저장 실패");
        }

        return count;
    }

}
