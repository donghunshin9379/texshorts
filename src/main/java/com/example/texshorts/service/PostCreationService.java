package com.example.texshorts.service;

import com.example.texshorts.dto.PostCreateRequest;
import com.example.texshorts.dto.message.PostCreationMessage;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCreationService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagHubService tagHubService;
    private final TagParserUtils tagParserUtils;

    public void createPostFromMessage(PostCreationMessage msg) {
        PostCreateRequest dto = msg.getPostCreateRequest();
        Long userId = msg.getUserId();

        List<String> tagList = tagParserUtils.parseTagsToList(dto.getTags());
        String tagsWithHash = tagParserUtils.formatTagsWithHash(tagList);

        User user = userRepository.getReferenceById(userId);

        Post post = Post.builder()
                .thumbnailPath(msg.getThumbnailPath())
                .title(dto.getTitle())
                .content(dto.getContent())
                .location(dto.getLocation())
                .visibility(dto.getVisibility())
                .user(user)
                .createdAt(LocalDateTime.now())
                .viewCount(0)
                .tags(tagsWithHash)
                .build();

        tagHubService.registerOrUpdateTagUsage(tagList);
        postRepository.save(post);
    }


    public String generateThumbnailUrl(String fileName) {
         return "http://localhost:8080/thumbnails/" + fileName;
    }

}
