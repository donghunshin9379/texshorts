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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostFeedService {

    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final UserInterestTagService userInterestTagService;

    private static final Logger logger = LoggerFactory.getLogger(PostFeedService.class);

    @Value("${app.server-url}")
    private String serverUrl;

    /**
     * combined feed with page slice from cache
     */
    public List<PostResponseDTO> getCombinedFeed(int page, int size, Long userId) {
        int fetchSize = size * 5; // 충분히 큰 수로 캐시에 미리 확보

        int latestSize = fetchSize / 4;
        int popularSize = fetchSize / 2;
        int personalizedSize = fetchSize - latestSize - popularSize;

        List<PostResponseDTO> latest = getFeedFromCacheOrDb(FeedType.LATEST, latestSize, userId);
        List<PostResponseDTO> popular = getFeedFromCacheOrDb(FeedType.POPULAR, popularSize, userId);
        List<PostResponseDTO> personalized = getFeedFromCacheOrDb(FeedType.PERSONALIZED, personalizedSize, userId);

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

    private List<PostResponseDTO> getFeedFromCacheOrDb(FeedType type, int fetchSize, Long userId) {
        String cacheKey = type.name() + "_POST_LIST";

        // 1. 캐시에서 가져오기
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(0, fetchSize, cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return filterSeenPosts(cached, userId);
        }

        // 2. DB에서 가져오기
        List<Post> posts = fetchPostsFromDb(type, fetchSize, userId);

        // DTO 변환
        List<PostResponseDTO> dtos = posts.stream()
                .map(post -> toDto(post, serverUrl))
                .toList();

        // 캐시에 저장
        redisCacheService.cachePostList(0, dtos.size(), dtos, cacheKey);

        return filterSeenPosts(dtos, userId);
    }

    private List<Post> fetchPostsFromDb(FeedType type, int size, Long userId) {
        switch (type) {
            case LATEST:
                return postRepository.findDistinctLatestPosts(
                        PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).getContent();
            case POPULAR:
                return postRepository.findDistinctPopularPosts(
                        PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "viewCount")
                                .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                                .and(Sort.by(Sort.Direction.DESC, "commentCount")))
                ).getContent();
            case PERSONALIZED:
                if (userId == null) {
                    return postRepository.findDistinctPopularPosts(
                            PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "viewCount")
                                    .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                                    .and(Sort.by(Sort.Direction.DESC, "commentCount")))
                    ).getContent();
                } else {
                    List<String> tags = userInterestTagService.getUserInterestTags(userId);
                    return postRepository.findDistinctPostsByTagNames(tags, PageRequest.of(0, size));
                }
            default:
                return List.of();
        }
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

    /** Post -> DTO */
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
