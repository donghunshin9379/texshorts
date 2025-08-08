package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.service.UserInterestTagService;
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
@RequestMapping("/api/user/activity")
@RequiredArgsConstructor
public class UserActivityController { /**유저 활동 트리거 */

    private final ViewService viewService;
    private static final Logger logger = LoggerFactory.getLogger(UserActivityController.class);

    /** 일정 시간 초과 시청 호출 */
    @PostMapping("/watch-long")
    public ResponseEntity<Void> recordLongView(
            @RequestParam Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        viewService.increaseViewCountIfNotViewed(postId, userId);
        return ResponseEntity.ok().build();
    }

//    /** 유저 액션 기반 호출 (서비스 내부에서 메소드 호출로 해결 가능 별도API X*/
//    @PostMapping("/interact")
//    public ResponseEntity<Void> recordInteractionView(
//            @RequestParam Long postId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        viewService.increaseViewCountIfNotViewed(postId, userDetails.getUserId());
//
//        return ResponseEntity.ok().build();
//    }



}
