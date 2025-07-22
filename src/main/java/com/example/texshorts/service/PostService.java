package com.example.texshorts.service;

import com.example.texshorts.DTO.PostCreateRequest;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.entity.ViewHistory;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.repository.ViewHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {
    // application.properties 에서 경로 주입
    @Value("${app.upload.dir}")
    private String uploadDir;
    private final PostRepository postRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final UserRepository userRepository;
    private final TagHubService tagHubService;
    private final TagParserUtils tagParserUtils;
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    private final PostDeletionQueueService postDeletionQueueService;

    // 게시물 객체 생성
    @Transactional
    public void buildPost(MultipartFile thumbnail, PostCreateRequest dto, Long userId) {
        try {
            String savedFileName = saveThumbnail(thumbnail);
            // TagHub용
            List<String> tagList = tagParserUtils.parseTagsToList(dto.getTags());

            // Post용
            String tagsWithHash = tagParserUtils.formatTagsWithHash(tagList);

            // 사용자 엔티티 프록시
            User user = userRepository.getReferenceById(userId);

            //posts 테이블 객체
            Post post = Post.builder()
                    .thumbnailPath(savedFileName)
                    .title(dto.getTitle())
                    .content(dto.getContent())
                    .location(dto.getLocation())
                    .visibility(dto.getVisibility())
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .viewCount(0)
                    .tags(tagsWithHash)
                    .build();

            // tag_hub 테이블 저장
            tagHubService.registerOrUpdateTagUsage(tagList);
            //posts 테이블 저장
            postRepository.save(post);

        } catch (IOException e) {
            throw new RuntimeException("게시물 생성 실패", e);
        }
    }

    public String saveThumbnail(MultipartFile thumbnail) throws IOException {
        String fileName = UUID.randomUUID() + "_" + thumbnail.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(thumbnail.getInputStream(), filePath);
        return fileName;
    }

    /**
     * 게시물 리스트 페이징 (세로 스와이프)
     * page : 몇번쨰 게시물 페이지
     * size : 가져올 게시물 수량
     * */
//    public List<PostResponseDTO> getPostsPaged(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//        Page<Post> postPage = postRepository.findAll(pageable);
//
//        return postPage.getContent().stream()
//                .map(PostResponseDTO::new)
//                .toList();
//    }

    // 게시물 조회수 증가( 첫 시청filter)
    @Transactional
    public void increaseViewCountIfNotViewed(Long postId, Long userId) {
        boolean alreadyViewed = viewHistoryRepository.existsByUserIdAndPostId(userId, postId);
        if (!alreadyViewed) {
            // 조회수 증가
            postRepository.incrementViewCount(postId);

            // 조회 기록 추가
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다. id=" + postId));

            viewHistoryRepository.save(new ViewHistory(userId, post, LocalDateTime.now()));
        }
    }

    // 게시물 객체 삭제 (유저확인)
    @Transactional
    public void requestDeletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시물이 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 게시물이 아닙니다.");
        }

        post.setDeleted(true); //soft 삭제
        postRepository.save(post);

        postDeletionQueueService.enqueuePostForDeletion(postId); // Redis에 등록
    }









}


