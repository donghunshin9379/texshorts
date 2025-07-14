package com.example.texshorts.service;

import com.example.texshorts.DTO.PostCreateRequest;
import com.example.texshorts.DTO.PostResponseDTO;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service

public class PostService {
    // application.properties 에서 경로 주입
    @Value("${app.upload.dir}")
    private String uploadDir;
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }
    
    // 게시물 객체 생성
    public void buildPost(MultipartFile thumbnail, PostCreateRequest dto, User user) {
        try {
            String savedFileName = saveThumbnail(thumbnail);

            Post post = Post.builder()
                    .thumbnailPath(savedFileName)
                    .title(dto.getTitle())
                    .content(dto.getContent())
                    .tags(dto.getTags())
                    .location(dto.getLocation())
                    .visibility(dto.getVisibility())
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();

            uploadPost(post);
        } catch (IOException e) {
            throw new RuntimeException("게시물 생성 실패", e);
        }
    }

    // 게시물 객체 > DB
    @Transactional
    public void uploadPost(Post post) {
        postRepository.save(post);
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

    public List<PostResponseDTO> getPostsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findAll(pageable);

        return postPage.getContent().stream()
                .map(PostResponseDTO::new)
                .toList();
    }




}
