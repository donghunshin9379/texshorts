package com.example.texshorts.service;

import com.example.texshorts.dto.CommentListResponseDTO;
import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.ReplyCommentListResponseDTO;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RedisCacheService redisCacheService;

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    /** 루트 댓글 조회 (캐시 + DB) */
    public CommentListResponseDTO getComments(Long postId, Long lastCommentId) {
        if (lastCommentId == null) {
            List<CommentResponseDTO> cached = redisCacheService.getRootCommentList(postId);
            if (!cached.isEmpty()) return new CommentListResponseDTO(cached);

            List<CommentResponseDTO> dtos = commentRepository.findRootCommentDTOs(postId);
            redisCacheService.cacheRootComments(postId, dtos);
            return new CommentListResponseDTO(dtos);
        } else {
            List<CommentResponseDTO> dtos = commentRepository.findRootCommentsAfter(postId, lastCommentId);
            return new CommentListResponseDTO(dtos);
        }
    }

    /** 답글 목록 조회 (캐시 + DB) */
    public ReplyCommentListResponseDTO getReplies(Long parentCommentId, Long lastReplyId) {
        if (lastReplyId == null) {
            List<CommentResponseDTO> cached = redisCacheService.getReplieCommentList(parentCommentId);

            if (cached != null && !cached.isEmpty()) {
                return new ReplyCommentListResponseDTO(cached);
            }

            List<CommentResponseDTO> dtoList = commentRepository.findReplyDTOs(parentCommentId);
            redisCacheService.cacheReplies(parentCommentId, dtoList);
            return new ReplyCommentListResponseDTO(dtoList);
        } else {
            List<CommentResponseDTO> dtoList = commentRepository.findRepliesAfter(parentCommentId, lastReplyId);
            return new ReplyCommentListResponseDTO(dtoList);
        }
    }


    public int getCommentCount(Long postId) {
        return commentRepository.countByPostIdAndParentIsNullAndIsDeletedFalse(postId);
    }

    public int getReplyCount(Long parentCommentId) {
        return commentRepository.countByParentIdAndIsDeletedFalse(parentCommentId);
    }

    // 댓글ID 모음으로 검색
    public List<CommentResponseDTO> getCommentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Comment> comments = commentRepository.findByIdIn(ids);

        // 캐싱된 순서대로 정렬 (DB 조회는 순서 보장 안 됨)
        Map<Long, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        return ids.stream()
                .map(commentMap::get)
                .filter(Objects::nonNull)
                .map(CommentResponseDTO::fromEntity) // DTO 변환
                .collect(Collectors.toList());
    }

    public List<CommentResponseDTO> getCachedRootComments(Long postId) {
        return redisCacheService.getRootCommentList(postId);
    }

}
