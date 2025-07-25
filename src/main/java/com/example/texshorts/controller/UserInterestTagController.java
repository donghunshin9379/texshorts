package com.example.texshorts.controller;

import com.example.texshorts.service.RequestRedisQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 호출 타이밍 연계 필요 */ //게시물 시청 기준으로 호출해야할듯. 기준은 ViewHistory 특정 유저의 게시물 기록 10개 쌓임> 게시물의 각태그 읽고 추가
@RestController
@RequestMapping("/api/user/interest-tags")
@RequiredArgsConstructor
public class UserInterestTagController {

    private final RequestRedisQueue requestRedisQueue;

    @PostMapping("/add")
    public ResponseEntity<String> addInterestTag(@RequestParam Long userId, @RequestParam String tagName) {
        requestRedisQueue.enqueueUserInterestTagUpdate(userId, tagName, "add");
        return ResponseEntity.ok("관심태그 추가 요청이 큐에 저장되었습니다.");
    }

    @PostMapping("/remove") /**갱신하기 OR 삭제 + 생성하기 고민 */
    public ResponseEntity<String> removeInterestTag(@RequestParam Long userId, @RequestParam String tagName) {
        requestRedisQueue.enqueueUserInterestTagUpdate(userId, tagName, "remove");
        return ResponseEntity.ok("관심태그 삭제 요청이 큐에 저장되었습니다.");
    }

}
