package com.example.texshorts.controller;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.service.CommentReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments/reaction")
@RequiredArgsConstructor
public class CommentReactionController {

    private final CommentReactionService commentReactionService;

    //댓글 좋아요 등록
    @PostMapping("/doLike")
    public ResponseEntity<Void> like(@RequestParam Long commentId,
                                     @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();

        commentReactionService.react(commentId, user, ReactionType.LIKE);
        return ResponseEntity.ok().build();
    }

    //댓글 싫어요 등록
    @PostMapping("/doDislike")
    public ResponseEntity<Void> dislike(@RequestParam Long commentId,
                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();

        commentReactionService.react(commentId, user, ReactionType.DISLIKE);
        return ResponseEntity.ok().build();
    }

}
