package com.example.texshorts.service;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostReaction;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.PostReactionRepository;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostReactionService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final RedisCacheService redisCacheService;
    private final RequestRedisQueue requestRedisQueue;

    private final Logger logger = LoggerFactory.getLogger(PostReactionService.class);

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
            logger.info("deleteReaction 완료 postId: {}", postId);

        }
        saveNewReaction(post, user, type, postId);
    }

    private void deleteReaction(PostReaction reaction, Long postId, ReactionType type) {
        logger.info("deleteReaction 호출됨");
        postReactionRepository.deleteById(reaction.getId());

        redisCacheService.decrementPostReactionCount(postId, type);

        // 좋아요 수 감소 시, likeCount DB 반영 요청 큐에 추가
        if (type == ReactionType.LIKE) {
            requestRedisQueue.enqueueLikeCountUpdate(postId);
        }
    }

    private void saveNewReaction(Post post, User user, ReactionType type, Long postId) {
        PostReaction newReaction = new PostReaction();
        newReaction.setPost(post);
        newReaction.setUser(user);
        newReaction.setType(type);
        postReactionRepository.save(newReaction);

        redisCacheService.incrementPostReactionCount(postId, type);

        // 좋아요 수 증가 시, likeCount DB 반영 요청 큐에 추가
        if (type == ReactionType.LIKE) {
            requestRedisQueue.enqueueLikeCountUpdate(postId);
        }
    }

    public Long getLikeCount(Long postId) {
        return redisCacheService.getPostReactionCount(postId, ReactionType.LIKE,
                () -> postReactionRepository.countByPostIdAndType(postId, ReactionType.LIKE));
    }



}
