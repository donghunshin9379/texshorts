package com.example.texshorts.component;

import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CommentCountFlusher {
    private final RedisCacheService redisCacheService;
    private final PostRepository postRepository;
    private static final Logger logger = LoggerFactory.getLogger(CommentCountFlusher.class);

    // Redis -> DB
    @Transactional
    public void flushCommentCountToDatabase(Long postId) {
        String key = "post:commentCount:" + postId;
        String countStr = redisCacheService.get(key);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                postRepository.updateCommentCount(postId, count);
            } catch (NumberFormatException e) {
                logger.warn("댓글 카운트 파싱 실패 for postId {} : {}", postId, e.getMessage());
            }
        }
    }

}
