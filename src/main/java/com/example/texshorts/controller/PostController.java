package com.example.texshorts.controller;

import com.example.texshorts.dto.PostCreateRequest;
import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.entity.Post;
import com.example.texshorts.service.PostService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(@RequestPart("thumbnail") MultipartFile thumbnail,
                                        @RequestPart("data") String dataJson,
                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        PostCreateRequest dto = mapper.readValue(dataJson, PostCreateRequest.class);

        postService.requestCreatePost(thumbnail, dto, customUserDetails.getUserId());
        return ResponseEntity.ok("게시물 생성 성공");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePost(@RequestParam Long postId,
                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        postService.requestDeletePost(postId, customUserDetails.getUserId());
        return ResponseEntity.ok("게시물이 삭제되었습니다.");
    }

    @GetMapping("/mine")
    public ResponseEntity<List<PostResponseDTO>> getMyPost(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Post> myPostsPage = postService.getPostsByUserEntities(
                customUserDetails.getUserId(), page, size
        );

        List<PostResponseDTO> myPostsList = myPostsPage.getContent().stream()
                .map(PostResponseDTO::fromEntityWithoutThumbnail)
                .toList();

        return ResponseEntity.ok(myPostsList);
    }





}