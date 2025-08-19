package com.example.texshorts.component;

import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.service.PostFeedService;
import com.example.texshorts.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**인기 카테고리 확장 예비용*/
@Component
@RequiredArgsConstructor
public class PopularFeedRefresher {
    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final PostFeedService postFeedService;

    public void refreshPopularFeedCache() {
        int page = 0;
        int size = 20;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "viewCount")
                        .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                        .and(Sort.by(Sort.Direction.DESC, "commentCount"))
        );

        String serverUrl = "http://localhost:8080";

        List<PostResponseDTO> popularPosts = postRepository.findAll(pageable).stream()
                .map(post -> postFeedService.toDto(post, serverUrl))
                .toList();


        redisCacheService.cachePostList(page, size, popularPosts, RedisCacheService.POPULAR_POST_LIST_KEY_PREFIX);
    }

}
