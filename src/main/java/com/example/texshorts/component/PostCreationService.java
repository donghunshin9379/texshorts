package com.example.texshorts.component;

import com.example.texshorts.dto.PostCreateRequest;
import com.example.texshorts.dto.message.PostCreationMessage;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.service.TagHubService;
import com.example.texshorts.service.TagParserUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCreationService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagHubService tagHubService;
    private final TagParserUtils tagParserUtils;

    private static final Logger logger = LoggerFactory.getLogger(PostCreationService.class);
    
    @Transactional
    public void createPostFromMessage(PostCreationMessage msg) {
        logger.info("RedisQueueWorker createPostFromMessage실행@@@@@@@@");

        PostCreateRequest dto = msg.getPostCreateRequest();
        Long userId = msg.getUserId();

        List<String> tagList = tagParserUtils.parseTagsToList(dto.getTags());
        String tagsWithHash = tagParserUtils.formatTagsWithHash(tagList);
        String thumbnailUrl = generateThumbnailUrl(msg.getThumbnailPath());

        User user = userRepository.getReferenceById(userId);

        Post post = Post.builder()
                .thumbnailPath(thumbnailUrl)
                .title(dto.getTitle())
                .content(dto.getContent())
                .location(dto.getLocation())
                .visibility(dto.getVisibility())
                .user(user)
                .createdAt(LocalDateTime.now())
                .tags(tagsWithHash)
                .build();

        tagHubService.registerOrUpdateTagUsage(tagList);
        postRepository.save(post);
    }


    public String generateThumbnailUrl(String fileName) {
         return "http://localhost:8080/thumbnails/" + fileName;
    }

}
