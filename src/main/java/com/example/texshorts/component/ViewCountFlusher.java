package com.example.texshorts.component;

import com.example.texshorts.entity.PostView;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.PostViewRepository;
import com.example.texshorts.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 캐시 -> DB */
@Service
@RequiredArgsConstructor
public class ViewCountFlusher {

    private final RedisCacheService redisCacheService;
    private final PostViewRepository postViewRepository;
    private final PostRepository postRepository;

    @Transactional
    public void flushViewCountToDatabase(Long postId) {
        Long count = redisCacheService.getViewCount(postId); // Redis count
        if (count == null) return;

        PostView postView = postViewRepository.findByPostId(postId) // DB 조회
                .orElseGet(() -> {
                    PostView pv = new PostView();
                    pv.setPostId(postId);
                    return pv;
                });

        postView.setViewCount(count.intValue()); // 엔티티 적용
        postViewRepository.save(postView); // DB저장

        /** Post 테이블 조회수 반영*/
        postRepository.updateViewCount(postId, count.intValue());

    }

}

