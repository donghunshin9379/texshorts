package com.example.texshorts.service;

import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.FeedType;
import com.example.texshorts.entity.ReactionType;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFeedService {

    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final UserInterestTagService userInterestTagService;

    private static final Logger logger = LoggerFactory.getLogger(PostFeedService.class);

    @Value("${app.server-url}")
    private String serverUrl;

    /** 통합 피드 가져오기 */
    public List<PostResponseDTO> getCombinedFeed(int page, int size, Long userId) {
        logger.info("getCombinedFeed 호출 page: {}, size:{}, userId:{}", page, size, userId);
        int fetchSize = Math.max(1, size * 5);

        Map<FeedType, Integer> feedSizeMap = Map.of(
                FeedType.LATEST, Math.max(1, fetchSize / 4),
                FeedType.POPULAR, Math.max(1, fetchSize / 2),
                FeedType.PERSONALIZED, Math.max(1, fetchSize - Math.max(1, fetchSize / 4) - Math.max(1, fetchSize / 2))
        );

        List<PostResponseDTO> combined = feedSizeMap.entrySet().stream()
                .flatMap(entry -> getFeedByType(entry.getKey(), entry.getValue(), userId).stream())
                .collect(Collectors.toMap(
                        PostResponseDTO::getPostId,
                        dto -> dto,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(Collectors.toCollection(ArrayList::new)); // mutable 리스트 생성


        logger.info("중복 제거 후 combined size: {}", combined.size());

        Collections.shuffle(combined);

        int from = page * size;
        int to = Math.min(from + size, combined.size());
        if (from >= combined.size()) return List.of();

        return combined.subList(from, to);
    }

    /** FeedType별 피드 가져오기 */
    private List<PostResponseDTO> getFeedByType(FeedType type, int fetchSize, Long userId) {
        prepareFeedCache(type, fetchSize, userId);
        return getFeedFromCache(type, fetchSize, userId);
    }

    /** 캐시 준비 */
    private void prepareFeedCache(FeedType type, int fetchSize, Long userId) {
        String cacheKeyPrefix = type.name() + "_POST_LIST";
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(0, fetchSize, cacheKeyPrefix);

        if (cached.isEmpty()) {
            List<Post> posts = fetchPostsFromDb(type, fetchSize, userId);
            List<PostResponseDTO> dtos = posts.stream()
                    .map(post -> toDto(post, serverUrl, userId)) // userId 전달
                    .toList(); // attachRealtimeCounts는 이미 toDto 내부에서 호출됨


            redisCacheService.cachePostList(0, fetchSize, dtos, cacheKeyPrefix);
            logger.info("DB에서 캐시 생성, FeedType: {}, size: {}", type, dtos.size());
        }
    }

    /** DB에서 Feed 가져오기 */
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
                if (userId == null) return fetchPostsFromDb(FeedType.POPULAR, size, null);
                List<String> tags = userInterestTagService.getUserInterestTags(userId);
                return postRepository.findDistinctPostsByTagNames(tags, PageRequest.of(0, size));
            default:
                return List.of();
        }
    }

    /** 실시간 카운트 attach */
    private PostResponseDTO attachRealtimeCounts(PostResponseDTO dto) {
        Long postId = dto.getPostId();

        // 좋아요
        Long likeCount = redisCacheService.getPostReactionCount(
                postId, ReactionType.LIKE,
                () -> postRepository.findById(postId).map(post -> (long) post.getLikeCount()).orElse(0L)
        );
        dto.setLikeCount(likeCount.intValue());

        // 댓글/답글
        dto.setCommentCount(redisCacheService.getRootCommentCount(postId));
        dto.setReplyCount(redisCacheService.getReplyCount(postId));

        // 조회수
        Long viewCount = redisCacheService.getViewCount(postId);
        dto.setViewCount(viewCount != null ? viewCount.intValue() : 0);

        return dto;
    }

    /** 캐시에서 Feed 가져오기 */
    private List<PostResponseDTO> getFeedFromCache(FeedType type, int fetchSize, Long userId) {
        String cacheKeyPrefix = type.name() + "_POST_LIST";
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(0, fetchSize, cacheKeyPrefix);

        if (cached.isEmpty()) return List.of();
        return filterSeenPosts(cached, userId);
    }

    /** 본 게시물 필터링 */
    private List<PostResponseDTO> filterSeenPosts(List<PostResponseDTO> posts, Long userId) {
        if (userId == null) return posts;

        Set<String> seenSet = Optional.ofNullable(redisCacheService.getSeenFeedPostIds(userId))
                .orElse(Collections.emptySet());

        return posts.stream()
                .filter(p -> !seenSet.contains(String.valueOf(p.getPostId())))
                .toList();
    }

    /** Post -> DTO 변환 */
    public PostResponseDTO toDto(Post post, String serverUrl, Long userId) {
        String thumbnailPath = post.getThumbnailPath();
        if (!(thumbnailPath.startsWith("http://") || thumbnailPath.startsWith("https://"))) {
            thumbnailPath = serverUrl + "/thumbnails/" + thumbnailPath;
        }

        PostResponseDTO dto = new PostResponseDTO();
        dto.setPostId(post.getId());
        dto.setNickname(post.getUser().getNickname());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setThumbnailUrl(thumbnailPath);
        dto.setCreatedAt(post.getCreatedAt());
        dto.setContentLines(contentToLines(post.getContent()));
        dto.setTags(post.getTags());

        // 실시간 카운트 attach
        attachRealtimeCounts(dto);

        // ✅ 캐시 기반 댓글/답글 ID 세팅
        dto.setRootCommentIds(redisCacheService.getRootCommentIds(post.getId()));
        dto.setReplyIds(redisCacheService.getReplyIds(post.getId()));

        // ✅ 유저별 리액션 상태 세팅
        if (userId != null) {
            dto.setUserLikedMap(Map.of(userId, redisCacheService.hasUserLiked(post.getId(), userId)));
            dto.setUserDislikedMap(Map.of(userId, redisCacheService.hasUserDisliked(post.getId(), userId)));
        }

        return dto;
    }


    /** 본 게시물 TTL 처리 */
    public void markPostAsSeen(Long userId, Long postId) {
        redisCacheService.cacheSeenFeedPost(userId, postId);
    }

    /** 줄 단위 content 처리 */
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
