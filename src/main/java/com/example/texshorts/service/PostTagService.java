package com.example.texshorts.service;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostTag;
import com.example.texshorts.entity.TagHub;
import com.example.texshorts.repository.PostTagRepository;
import com.example.texshorts.repository.TagHubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostTagService {

    private final PostTagRepository postTagRepository;
    private final TagHubRepository tagHubRepository;

    @Transactional
    public void linkPostAndTag(Post post, String tagName) {
        TagHub tagHub = tagHubRepository.findByTagName(tagName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다: " + tagName));

        boolean exists = postTagRepository.existsByPostAndTagHub(post, tagHub);
        if (!exists) {
            PostTag postTag = PostTag.builder()
                    .post(post)
                    .tagHub(tagHub)
                    .build();
            postTagRepository.save(postTag);
        }
    }

    @Transactional
    public void unlinkPostAndTags(Post post) {
        List<PostTag> postTags = postTagRepository.findByPostId(post.getId());

        for (PostTag postTag : postTags) {
            TagHub tagHub = postTag.getTagHub();
            tagHub.decrementUsageCount();

            if (tagHub.getUsageCount() <= 0) {
                tagHubRepository.delete(tagHub);
            }

            postTagRepository.delete(postTag);
        }
    }



}
