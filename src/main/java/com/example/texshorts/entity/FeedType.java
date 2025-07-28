package com.example.texshorts.entity;

public enum FeedType {
    LATEST,     // 최신순
    POPULAR,    // 인기순
    PERSONALIZED; // 개인화

    public static FeedType from(String raw) {
        try {
            return FeedType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("지원하지 않는 피드 타입: " + raw);
        }
    }

}
