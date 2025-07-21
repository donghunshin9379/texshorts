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
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    
    // 댓글 생성
    @PostMapping("/create")
    public ResponseEntity<Void> createComment(
            @RequestParam Long postId,
            @RequestParam(required = false) Long parentId,
            @RequestBody String content,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.createComment(postId, parentId, userDetails.getUser(), content);
        return ResponseEntity.ok().build();
    }

    // 게시글의 댓글 목록 조회
    @GetMapping("/get")
    public ResponseEntity<List<CommentResponseDTO>> getComments(@RequestParam Long postId) {
        List<Comment> comments = commentService.getComments(postId);
        List<CommentResponseDTO> response = comments.stream()
                .map(this::convertToDTOWithReplies)
                .collect(Collectors.toList());

        if (response.isEmpty()) {
            return ResponseEntity.status(204).build();  // 댓글 없음
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getCommentCount(@RequestParam Long postId) {
        int count = commentService.getCommentCountCached(postId);
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



    // 답글 포함 DTO 변환
    private CommentResponseDTO convertToDTOWithReplies(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO(comment);
        List<CommentResponseDTO> replies = comment.getReplies().stream()
                .filter(reply -> !reply.getIsDeleted()) //삭제된 답글 제외
                .map(CommentResponseDTO::new)
                .collect(Collectors.toList());
        dto.setReplies(replies);
        return dto;
    }


}