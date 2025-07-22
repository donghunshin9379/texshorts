package com.example.texshorts.controller;

import com.example.texshorts.DTO.CommentResponseDTO;
import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    // 댓글 생성 (루트)
    @PostMapping("/create")
    public ResponseEntity<Void> createRootComment(
            @RequestParam Long postId,
            @RequestBody String content,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.createRootComment(postId, userDetails.getUser(), content);
        return ResponseEntity.ok().build();
    }

    // 답글(대댓글) 생성
    @PostMapping("/reply")
    public ResponseEntity<?> createReplyComment(
            @RequestParam Long parentCommentId,
            @RequestBody String content,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Comment comment = commentService.createReply(parentCommentId, userDetails.getUser(), content);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            // 임시 예외처리
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 게시글의 댓글 목록 조회
    @GetMapping("/get/root")
    public ResponseEntity<List<CommentResponseDTO>> getRootComments(@RequestParam Long postId) {
        List<CommentResponseDTO> response = commentService.getRootComments(postId);

        if (response.isEmpty()) {
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.ok(response);
    }

    // 게시글 댓글의 답글(대댓글) 목록 조회
    @GetMapping("/get/replies")
    public ResponseEntity<List<CommentResponseDTO>> getReplyComments(@RequestParam Long commentId) {
        List<CommentResponseDTO> response = commentService.getReplies(commentId);

        if (response.isEmpty()) {
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.ok(response);
    }

    // 댓글 갯수
    @GetMapping("/count/root")
    public ResponseEntity<Integer> getCommentCount(@RequestParam Long postId) {
        int count = commentService.getCommentCountCached(postId);
        return ResponseEntity.ok(count);
    }
    
    // 답글 갯수
    @GetMapping("/count/replies")
    public ResponseEntity<Integer> getReplyCountCached(@RequestParam Long postId) {
        int count = commentService.getReplyCountCached(postId);
        return ResponseEntity.ok(count);
    }


    // 댓글 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteComment(
            @RequestParam Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            commentService.deleteComment(id, userDetails.getUser());
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

}