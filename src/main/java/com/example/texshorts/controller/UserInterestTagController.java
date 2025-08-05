package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.entity.TagActionType;
import com.example.texshorts.service.RequestRedisQueue;
import com.example.texshorts.service.UserInterestTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 호출 타이밍 연계 필요 */ //게시물 시청 기준으로 호출해야할듯. 기준은 ViewHistory 특정 유저의 게시물 기록 10개 쌓임> 게시물의 각태그 읽고 추가
@RestController
@RequestMapping("/api/user/interest-tags")
@RequiredArgsConstructor
public class UserInterestTagController {

    private final RequestRedisQueue requestRedisQueue;
    private final UserInterestTagService userInterestTagService;

    // 현재 로그인한 유저 기준 관심태그 조회 (예: JWT 인증 등에서 userId 추출)
    @GetMapping
    public ResponseEntity<List<String>> getUserInterestTags(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        List<String> tags = userInterestTagService.getUserInterestTags(userId);
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addInterestTag(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String tagName) {
        Long userId = userDetails.getUserId();
        requestRedisQueue.enqueueUserInterestTagUpdate(userId, tagName, TagActionType.ADD);

        return ResponseEntity.ok("관심태그 추가 요청이 큐에 저장되었습니다.");
    }

    @PostMapping("/remove") /**갱신하기 OR 삭제 + 생성하기 고민 */
    public ResponseEntity<String> removeInterestTag(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String tagName) {
        Long userId = userDetails.getUserId();
        requestRedisQueue.enqueueUserInterestTagUpdate(userId, tagName, TagActionType.REMOVE);
        return ResponseEntity.ok("관심태그 삭제 요청이 큐에 저장되었습니다.");
    }

}
