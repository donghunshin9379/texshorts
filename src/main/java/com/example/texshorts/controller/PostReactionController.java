package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.service.PostReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/posts/reaction")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('USER')") 클래스 단위 권한설정도 가능( 현재는 SecurityConfig)
public class PostReactionController {

    private final PostReactionService postReactionService;
    private final UserRepository userRepository;

    //포스트 좋아요 등록
    @PostMapping("/doLike")
    public ResponseEntity<Void> like(@RequestParam Long postId,
                                     @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();

        postReactionService.react(postId, user, ReactionType.LIKE);
        return ResponseEntity.ok().build();
    }

    //포스트 싫어요 등록
    @PostMapping("/doDislike")
    public ResponseEntity<Void> dislike(@RequestParam Long postId,
                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();

        postReactionService.react(postId, user, ReactionType.DISLIKE);
        return ResponseEntity.ok().build();
    }
    
    // 좋아요 카운트 get 요청 (
    @GetMapping("/like/count")
    public ResponseEntity<Long> getLikeCount(@RequestParam Long postId) {
        return ResponseEntity.ok(postReactionService.getLikeCount(postId));
    }




}
