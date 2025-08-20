package com.example.texshorts.service;

import com.example.texshorts.dto.PostCreateRequest;
import com.example.texshorts.dto.PostResponseDTO;
import com.example.texshorts.dto.message.PostCreationMessage;
import com.example.texshorts.dto.message.PostDeleteMessage;
import com.example.texshorts.entity.Post;
import com.example.texshorts.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PostService {
    // application.properties 에서 경로 주입
    @Value("${app.upload.dir}")
    private String uploadDir;
    private final PostRepository postRepository;
    private final RequestRedisQueue requestRedisQueue;

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    /** 게시물 생성 요청*/
    @Transactional
    public void requestCreatePost(MultipartFile thumbnail, PostCreateRequest dto, Long userId) {
        try {
            // 1. 썸네일 저장
            String savedFileName = saveThumbnail(thumbnail);

            // 2. 메시지 생성
            PostCreationMessage msg = new PostCreationMessage(savedFileName, dto, userId);

            // 3. 큐에 저장
            requestRedisQueue.enqueuePostCreation(msg);
        } catch (IOException e) {
            throw new RuntimeException("게시물 생성 실패", e);
        }
    }

    /** 썸네일 경로 */
    public String saveThumbnail(MultipartFile thumbnail) throws IOException {
        String fileName = UUID.randomUUID() + "_" + thumbnail.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir, "thumbnails");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(thumbnail.getInputStream(), filePath);
        return fileName;
    }

    /** 게시물 삭제 요청*/
    public void requestDeletePost(Long postId, Long userId) {
        // 게시물 존재 여부 확인
        postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("게시물이 없습니다."));
        // 큐 등록
        requestRedisQueue.enqueuePostDeletion(new PostDeleteMessage(postId, userId));
    }



    /** 나의 게시물 요청(특정 유저, 썸네일 제외용) */
    /** 마이페이지용: 썸네일 제외, 특정 유저 게시물 조회 */
    public Page<Post> getPostsByUserEntities(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByUserId(userId, pageable);
    }


}