package com.example.texshorts.controller;

import com.example.texshorts.dto.PostCreateRequest;
import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.service.PostFeedService;
import com.example.texshorts.service.PostService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") //swagger 인증용
public class PostController {
    private final PostService postService;
    private final PostFeedService postFeedService;

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponseDTO>> getPostsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postFeedService.getPostsPagedWithCache(page, size));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("data") String dataJson,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        PostCreateRequest dto = mapper.readValue(dataJson, PostCreateRequest.class);

        // userId만 전달
        postService.requestCreatePost(thumbnail, dto, customUserDetails.getUserId());
        return ResponseEntity.ok("게시물 생성 성공");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePost(
            @RequestParam Long postId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        postService.requestDeletePost(postId, customUserDetails.getUserId());
        logger.info("컨트롤러 삭제요청 ID : {}", customUserDetails.getUserId());
        return ResponseEntity.ok("게시물이 삭제되었습니다.");
    }

    // 조회수 증가 요청
    @PostMapping("/increase-view")
    public ResponseEntity<Void> increaseViewCount(
            @RequestParam Long postId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getId();

        postFeedService.increaseViewCountIfNotViewed(postId, userId);
        return ResponseEntity.ok().build();
    }

    // 홈탭 게시물 피드
//    @GetMapping("/feed/home")
//    public ResponseEntity<List<PostResponseDTO>> getHomeFeed(@AuthenticationPrincipal CustomUserDetails customUserDetails,
//                                            @RequestParam (defaultValue = "0") int page,
//                                            @RequestParam (defaultValue = "0") int size) {
//
//        Long userId = customUserDetails.getUserId();
//        List<PostResponseDTO> feed = feedService.generateHomeFeed(userId, page, size);
//        return ResponseEntity.ok(feed);
//    }

//    // 검색탭 게시물 피드
//    @GetMapping("/feed/explore")
//    public ResponseEntity<Void> getExploreFeed() {
//
//    }
//
//    // 구독 탭 게시물 피드
//    @GetMapping("/feed/subscriptions")
//    public ResponseEntity<Void> getSubscriptionsFeed() {
//
//    }




}