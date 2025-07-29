package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.service.ViewService;
import lombok.RequiredArgsConstructor;
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

    // 중복시청 확인 + 조회수 증가
    @PostMapping("/increase-view")
    public ResponseEntity<Void> increaseViewCount(
            @RequestParam Long postId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getId();

        viewService.increaseViewCountIfNotViewed(postId, userId);
        return ResponseEntity.ok().build();
    }

    /**GET 추가 필요 (조회수 get)*/

}
