package com.example.texshorts.component;

import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReactionCountFlusher {

    private final RedisCacheService redisCacheService;
    private final PostRepository postRepository;

    @Transactional
    public void flushLikeCountToDatabase(Long postId) {
        Long cachedCount = redisCacheService.getPostReactionCount(postId, ReactionType.LIKE);
        if (cachedCount != null) {
            postRepository.findById(postId).ifPresent(post -> {
                post.setLikeCount(cachedCount.intValue());
                postRepository.save(post);
            });/**다른 리액션타입 확장 필요(싫어요)*/
        }
    }
}
