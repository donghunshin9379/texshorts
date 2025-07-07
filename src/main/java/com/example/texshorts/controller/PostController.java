package com.example.texshorts.controller;

import com.example.texshorts.DTO.PostResponseDTO;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "tags", required = false) String tags,
            @RequestPart(value = "location", required = false) String location,
            @RequestPart(value = "visibility", required = false) String visibility,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        String username = userDetails.getUsername();
        logger.info("Authorization 헤더: {}", request.getHeader("Authorization"));

        // 예를 들어, UserService에서 User 엔티티 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        logger.info(">>> 받은 content:\n{}", content);
        postService.createPost(thumbnail, title, content, tags, location, visibility, user);
        return ResponseEntity.ok("게시물 생성 성공");
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> getPagedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<PostResponseDTO> posts = postService.getPostsPaged(page, size);
        return ResponseEntity.ok(posts);
    }



}