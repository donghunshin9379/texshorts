package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.service.ViewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/view")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;
    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);
    // 중복시청 확인 + 조회수 증가
    @PostMapping("/increase-view")
    public ResponseEntity<Void> increaseViewCount(
            @RequestParam Long postId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();

        viewService.increaseViewCountIfNotViewed(postId, userId);
        logger.info("increaseViewCount 기반 시청 기록: userId={}, postId={}", userId, postId);
        return ResponseEntity.ok().build();
    }

    /**GET 추가 필요 (조회수 get)*/

}
