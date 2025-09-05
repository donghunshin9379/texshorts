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

    @GetMapping("/getCombined")
    public ResponseEntity<List<PostResponseDTO>> getCombinedFeed(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        List<PostResponseDTO> combined = postFeedService.getCombinedFeed(page, size, userId);

        return ResponseEntity.ok(combined);
    }

    /** 시청여부 (캐시저장)*/
    @PostMapping("/seen")
    public ResponseEntity<Void> markPostAsSeen(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long postId
    ) {
        Long userId = userDetails.getUserId();
        postFeedService.markPostAsSeen(userId, postId);
        return ResponseEntity.ok().build();
    }
}
