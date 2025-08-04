package com.example.texshorts.repository;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.PostTag;
import com.example.texshorts.entity.TagHub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    List<PostTag> findByPostId(Long postId);

    boolean existsByPostAndTagHub(Post post, TagHub tagHub);

}
