package com.example.texshorts.service;

import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.FeedType;
import com.example.texshorts.entity.Post;
import com.example.texshorts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFeedService {

    private final PostRepository postRepository;
    private final RedisCacheService redisCacheService;
    private final UserInterestTagService userInterestTagService;
    private static final Logger logger = LoggerFactory.getLogger(PostFeedService.class);

    /** 피드 종류
     * LATEST: 최근 피드
     * POPULAR: 인기 피드
     * PERSONALIZED: 개인화 피드
     * */
    public List<PostResponseDTO> getFeed(FeedType type, int page, int size, Long userId) {
        return switch (type) {
            case LATEST -> getLatestPosts(page, size, userId);
            case POPULAR -> getPopularPosts(page, size, userId);
            case PERSONALIZED -> getPersonalizedPosts(page, size, userId);
        };
    }


    /**
     * 캐시 로직 흐름
     * 최초 호출 시점 캐시 == null
     * DB > 캐시 저장
     * 
     * 이후 호출 시점 캐시(저장됨)
     * 캐시 반환
     * */

    /** 최근 게시물 피드 */
    private List<PostResponseDTO> getLatestPosts(int page, int size, Long userId) {
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(page, size, RedisCacheService.LATEST_POST_LIST_KEY_PREFIX);
        if (cached == null) {
            logger.info("최근 피드 캐시 없음, DB에서 조회 시작");

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            cached = postRepository.findAll(pageable).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            redisCacheService.cachePostList(page, size, cached, RedisCacheService.LATEST_POST_LIST_KEY_PREFIX);
        }

        cached = filterSeenPosts(cached, userId);
        return cached;
    }

    /** 인기 게시물 피드 */
    private List<PostResponseDTO> getPopularPosts(int page, int size, Long userId) {
        List<PostResponseDTO> cached = redisCacheService.getCachedPostList(page, size, RedisCacheService.POPULAR_POST_LIST_KEY_PREFIX);
        if (cached == null) {
            logger.info("인기 피드 캐시 없음, DB에서 조회 시작");

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewCount"));
            cached = postRepository.findAll(pageable).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            redisCacheService.cachePostList(page, size, cached, RedisCacheService.POPULAR_POST_LIST_KEY_PREFIX);
        }

        cached = filterSeenPosts(cached, userId);
        return cached;
    }

    /** 개인화 게시물 피드 */ //관심태그 조회 캐시로
    private List<PostResponseDTO> getPersonalizedPosts(int page, int size, Long userId) {
        //로그인상태 아닐때 인기피드 대체
        if (userId == null) return getPopularPosts(page, size, null);

        //관심태그 조회 (관심태그 없을시 인기피드 대체)
        List<String> interestTags = userInterestTagService.getUserInterestTags(userId);
        if (interestTags.isEmpty()) return getPopularPosts(page, size, userId);

        // 관심태그 기반 Post조회 (중복X)
        Pageable pageable = PageRequest.of(0, size);
        List<Post> personalizedPosts = postRepository.findDistinctPostsByTagNames(interestTags, pageable);

        // 최종 개인화 post 부족시 > 인기피드 추가
        personalizedPosts = fillWithPopularPostsIfInsufficient(personalizedPosts, size);

        List<PostResponseDTO> dtos = personalizedPosts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return filterSeenPosts(dtos, userId);
    }


    /** 개인화 피드 부족 시 인기글 보충 */
    private List<Post> fillWithPopularPostsIfInsufficient(List<Post> posts, int size) {
        if (posts.size() >= size) return posts;

        int shortage = size - posts.size();
        Pageable pageable = PageRequest.of(0, shortage, Sort.by(Sort.Direction.DESC, "viewCount"));
        List<Post> fallback = postRepository.findAll(pageable).getContent();

        Set<Long> existingIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        List<Post> additional = fallback.stream()
                .filter(post -> !existingIds.contains(post.getId()))
                .collect(Collectors.toList());

        List<Post> result = new ArrayList<>(posts);
        result.addAll(additional);
        return result;
    }



    /** 노출 피드 중복방지 필터링 */
    private List<PostResponseDTO> filterSeenPosts(List<PostResponseDTO> posts, Long userId) {
        if (userId == null) return posts;
        Set<String> seenPostIds = redisCacheService.getSeenFeedPostIds(userId);
        if (seenPostIds == null || seenPostIds.isEmpty()) return posts;
        return posts.stream()
                .filter(post -> !seenPostIds.contains(String.valueOf(post.getPostId())))
                .collect(Collectors.toList());
    }


    /** 게시물 목록 조회 API 호출용 */
    public PostResponseDTO toDto(Post post) {
        List<String> lines = contentToLines(post.getContent());
        return new PostResponseDTO(
                post.getUser().getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getThumbnailPath(),
                post.getCreatedAt(),
                lines,
                post.getTags(),
                post.getId()
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
