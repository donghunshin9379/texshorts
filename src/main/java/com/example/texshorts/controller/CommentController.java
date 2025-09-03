package com.example.texshorts.controller;

import com.example.texshorts.dto.CommentListResponseDTO;
import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.dto.ReplyCommentListResponseDTO;
import com.example.texshorts.dto.message.CommentCreationMessage;
import com.example.texshorts.dto.message.CommentDeleteMessage;
import com.example.texshorts.dto.message.ReplyCommentCreationMessage;
import com.example.texshorts.service.CommentService;
import com.example.texshorts.service.RequestRedisQueue;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final RequestRedisQueue requestRedisQueue;

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    // 댓글 생성 (루트)
    @PostMapping("/create")
    public ResponseEntity<Void> createRootComment(@RequestParam Long postId,
                                                  @RequestBody String content,
                                                  @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        CommentCreationMessage msg = new CommentCreationMessage(postId, customUserDetails.getUserId(), content);
        requestRedisQueue.enqueueCommentCreation(msg);
        return ResponseEntity.ok().build();
    }

    // 답글(대댓글) 생성
    @PostMapping("/reply")
    public ResponseEntity<?> createReplyComment(@RequestParam Long parentCommentId,
                                                @RequestBody String content,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReplyCommentCreationMessage msg = new ReplyCommentCreationMessage(parentCommentId, userDetails.getUserId(), content);
        requestRedisQueue.enqueueReplyCommentCreation(msg);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteComment(@RequestParam Long id,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        CommentDeleteMessage msg = new CommentDeleteMessage(id, userDetails.getUserId());
        requestRedisQueue.enqueueCommentDeletion(msg);
        return ResponseEntity.ok().build();
    }

    // 댓글 목록 조회
    @GetMapping("/get/root")
    public ResponseEntity<CommentListResponseDTO> getRootComments(
            @RequestParam Long postId,
            @RequestParam(required = false) Long lastCommentId) {
        CommentListResponseDTO response = commentService.getComments(postId, lastCommentId);
        return ResponseEntity.ok(response);
    }

    // 답글 목록 조회
    @GetMapping("/get/replies")
    public ResponseEntity<ReplyCommentListResponseDTO> getReplies(
            @RequestParam Long parentCommentId,
            @RequestParam(required = false) Long lastReplyId) {
        ReplyCommentListResponseDTO response = commentService.getReplies(parentCommentId, lastReplyId);
        return ResponseEntity.ok(response);
    }

    // 댓글 갯수
    @GetMapping("/count/root")
    public ResponseEntity<Integer> getCommentCount(@RequestParam Long postId) {
        int count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }

    // 답글 갯수
    @GetMapping("/count/replies")
    public ResponseEntity<Integer> getReplyCount(@RequestParam Long parentCommentId) {
        int count = commentService.getReplyCount(parentCommentId);
        return ResponseEntity.ok(count);
    }


}


