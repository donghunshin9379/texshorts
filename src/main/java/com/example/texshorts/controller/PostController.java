package com.example.texshorts.controller;

import com.example.texshorts.DTO.PostCreateRequest;
import com.example.texshorts.DTO.PostResponseDTO;
import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.UserRepository;
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
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> getPagedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<PostResponseDTO> posts = postService.getPostsPaged(page, size);
        return ResponseEntity.ok(posts);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("data") String dataJson,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) throws JsonProcessingException {
        logger.info("Controller createPost 메소드 실행 ");
        ObjectMapper mapper = new ObjectMapper();
        PostCreateRequest dto = mapper.readValue(dataJson, PostCreateRequest.class);

        User user = customUserDetails.getUser(); //User 인스턴스 전체(**보안리팩토링필요**)
        postService.buildPost(thumbnail, dto, user);
        return ResponseEntity.ok("게시물 생성 성공");
    }





}