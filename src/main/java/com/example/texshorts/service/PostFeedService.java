package com.example.texshorts.service;

import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.ViewHistory;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFeedService {
    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final ViewHistoryRepository viewHistoryRepository;

    /** Redis 캐시된 게시물 조회/저장
     * 
     * */
    public List<PostResponseDTO> getPostsPagedWithCache(int page, int size) {
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(page, size);
        if (cached != null) return cached;

        List<PostResponseDTO> fresh = getPostsPaged(page, size);
        redisCacheService.cachePostList(page, size, fresh);
        return fresh;
    }

    /** DB > 캐시 저장 */
    public List<PostResponseDTO> getPostsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findAll(pageable).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void increaseViewCountIfNotViewed(Long postId, Long userId) {
        if (!viewHistoryRepository.existsByUserIdAndPostId(userId, postId)) {
            postRepository.incrementViewCount(postId);
            viewHistoryRepository.save(
                    new ViewHistory(userId, postRepository.getReferenceById(postId), LocalDateTime.now())
            );
        }
    }



    /** 게시물 목록 조회 API 호출용
     * */
    public PostResponseDTO toDto(Post post) {
        List<String> lines = contentToLines(post.getContent());
        return new PostResponseDTO(
                post.getUser().getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getThumbnailPath(),
                post.getCreatedAt(),
                lines,
                post.getTags()
        );
    }

    private List<String> contentToLines(String content) {
        if (content == null || content.isEmpty()) return List.of();

        List<String> rawLines = Arrays.stream(
                        content.replaceAll("\\r\\n", "\n")
                                .replaceAll("\n{3,}", "\n\n")
                                .split("\n"))
                .map(String::trim)
                .collect(Collectors.toList());

        return filterConsecutiveEmptyLines(rawLines);
    }

    private List<String> filterConsecutiveEmptyLines(List<String> lines) {
        List<String> filtered = new ArrayList<>();
        boolean lastLineWasEmpty = false;

        for (String line : lines) {
            if (line.isEmpty()) {
                if (!lastLineWasEmpty) {
                    filtered.add(line);
                    lastLineWasEmpty = true;
                }
            } else {
                filtered.add(line);
                lastLineWasEmpty = false;
            }
        }
        return filtered;
    }

}
