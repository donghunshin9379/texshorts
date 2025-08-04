package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.FeedType;
import com.example.texshorts.service.PostFeedService;
import com.example.texshorts.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final PostFeedService postFeedService;
    private final RedisCacheService redisCacheService;

    // 피드 게시물 요청( 타입별 분리)
    @GetMapping("/get")
    public ResponseEntity<List<PostResponseDTO>> getFeed(
            @RequestParam("type") String type,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        FeedType feedType = FeedType.from(type);
       /**개인화피드 로그인여부 리팩 고려*/
        Long userId = userDetails != null ? userDetails.getUserId() : null;

        List<PostResponseDTO> feed = postFeedService.getFeed(feedType, page, size, userId);
        return ResponseEntity.ok(feed);
    }

    // 피드 게시물 노출 기록 저장 (실시간 중복 필터용 현재 TTL : 1일 )
    /**서버 입장 중복피드노출 방지 (캐시만 저장 DB X)*/
    @PostMapping("/seen")
    public ResponseEntity<Void> markPostAsSeen(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long postId) {

        Long userId = userDetails.getUserId();
        redisCacheService.cacheSeenFeedPost(userId, postId);

        return ResponseEntity.ok().build();
    }


}
