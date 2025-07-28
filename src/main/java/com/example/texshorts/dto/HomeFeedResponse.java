package com.example.texshorts.dto;

import com.example.texshorts.entity.Post;
import lombok.Data;

import java.util.List;

@Data
public class HomeFeedResponse {
    private List<Post> recommendedPosts; // 개인화 추천 콘텐츠
    private List<Post> popularPosts;     // 인기 콘텐츠
    private List<Post> newPosts;         // 최신 콘텐츠
}
