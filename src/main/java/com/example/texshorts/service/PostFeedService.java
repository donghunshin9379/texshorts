package com.example.texshorts.service;

import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.FeedType;
import com.example.texshorts.entity.Post;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFeedService {

    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final UserInterestTagService userInterestTagService;

    private static final Logger logger = LoggerFactory.getLogger(PostFeedService.class);

    /**
     * combined feed (중복 제거 + 페이지 단위)
     */
    public List<PostResponseDTO> getCombinedFeed(int page, int size, Long userId) {
        // 피드 비율
        int latestSize = size / 4; //전체 1/4 (25%)
        int popularSize = size / 2; //전체 1/2 (50%)
        int personalizedSize = size - latestSize - popularSize; //나머지 1/4 (25%)

        List<PostResponseDTO> latest = getFeed(FeedType.LATEST, latestSize, userId);
        List<PostResponseDTO> popular = getFeed(FeedType.POPULAR, popularSize, userId);
        List<PostResponseDTO> personalized = getFeed(FeedType.PERSONALIZED, personalizedSize, userId);

        // 합치기 + 중복 제거
        Map<Long, PostResponseDTO> map = new LinkedHashMap<>();
        latest.forEach(dto -> map.putIfAbsent(dto.getPostId(), dto));
        popular.forEach(dto -> map.putIfAbsent(dto.getPostId(), dto));
        personalized.forEach(dto -> map.putIfAbsent(dto.getPostId(), dto));

        List<PostResponseDTO> combined = new ArrayList<>(map.values());
        Collections.shuffle(combined);

        // 페이지 슬라이스
        int from = page * size;
        int to = Math.min(from + size, combined.size());
        if (from >= combined.size()) return List.of();
        return combined.subList(from, to);
    }


    @Value("${app.server-url}")
    private String serverUrl;

    /**
     * 타입별 feed
     */
    private List<PostResponseDTO> getFeed(FeedType type, int size, Long userId) {
        String cacheKey = type.name() + "_POST_LIST";

        // 1. 캐시 조회
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(0, size, cacheKey);
        if (cached != null) {
            return filterSeenPosts(cached, userId);
        }

        // 2. DB 조회
        List<Post> posts;
        Pageable pageable = PageRequest.of(0, size);
        switch (type) {
            case LATEST -> posts = postRepository.findDistinctLatestPosts(
                    PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
            case POPULAR -> posts = postRepository.findDistinctPopularPosts(
                    PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "viewCount")
                            .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                            .and(Sort.by(Sort.Direction.DESC, "commentCount")))
            ).getContent();
            case PERSONALIZED -> {
                if (userId == null) {
                    posts = postRepository.findDistinctPopularPosts(
                            PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "viewCount")
                                    .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                                    .and(Sort.by(Sort.Direction.DESC, "commentCount")))
                    ).getContent();
                } else {
                    List<String> tags = userInterestTagService.getUserInterestTags(userId);
                    posts = postRepository.findDistinctPostsByTagNames(tags, PageRequest.of(0, size));
                }
            }
            default -> posts = List.of();
        }

        // 3. DTO 변환 + 캐싱
        List<PostResponseDTO> dtos = posts.stream()
                .map(post -> toDto(post, serverUrl))
                .toList();

        redisCacheService.cachePostList(0, dtos.size(), dtos, cacheKey);


        return filterSeenPosts(dtos, userId);
    }


    /** 본 게시물 필터링 */
    private List<PostResponseDTO> filterSeenPosts(List<PostResponseDTO> posts, Long userId) {
        if (userId == null) return posts;
        Set<String> seen = redisCacheService.getSeenFeedPostIds(userId);
        if (seen == null || seen.isEmpty()) return posts;
        return posts.stream()
                .filter(p -> !seen.contains(String.valueOf(p.getPostId())))
                .toList();
    }

    /** Post 엔티티 -> DTO */
    public PostResponseDTO toDto(Post post, String serverUrl) {
        String thumbnailPath = post.getThumbnailPath();

        if (!(thumbnailPath.startsWith("http://") || thumbnailPath.startsWith("https://"))) {
            thumbnailPath = serverUrl + "/thumbnails/" + thumbnailPath;
        }

        return new PostResponseDTO(
                post.getUser().getNickname(),
                post.getTitle(),
                post.getContent(),
                thumbnailPath,
                post.getCreatedAt(),
                contentToLines(post.getContent()),
                post.getTags(),
                post.getId()
        );
    }



    /** 본 게시물 TTL 처리 */
    public void markPostAsSeen(Long userId, Long postId) {
        redisCacheService.cacheSeenFeedPost(userId, postId);
    }

    // --- content 처리 ---
    private List<String> contentToLines(String content) {
        if (content == null || content.isEmpty()) return List.of();
        List<String> raw = Arrays.stream(content.replaceAll("\\r\\n", "\n")
                        .replaceAll("\n{3,}", "\n\n")
                        .split("\n"))
                .map(String::trim)
                .toList();
        return filterConsecutiveEmptyLines(raw);
    }

    private List<String> filterConsecutiveEmptyLines(List<String> lines) {
        List<String> filtered = new ArrayList<>();
        boolean lastEmpty = false;
        for (String line : lines) {
            if (line.isEmpty()) {
                if (!lastEmpty) filtered.add(line);
                lastEmpty = true;
            } else {
                filtered.add(line);
                lastEmpty = false;
            }
        }
        return filtered;
    }

}
