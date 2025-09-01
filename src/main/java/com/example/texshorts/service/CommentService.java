package com.example.texshorts.service;

import com.example.texshorts.dto.CommentListResponseDTO;
import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.ReplyCommentListResponseDTO;
import com.example.texshorts.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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
            if (!cached.isEmpty()) return new ReplyCommentListResponseDTO(cached);

            List<CommentResponseDTO> dtoList = commentRepository.findReplyDTOs(parentCommentId);
            redisCacheService.cacheReplies(parentCommentId, dtoList);
            return new ReplyCommentListResponseDTO(dtoList);
        } else {
            List<CommentResponseDTO> dtoList = commentRepository.findRepliesAfter(parentCommentId, lastReplyId);
            return new ReplyCommentListResponseDTO(dtoList);
        }
    }

    public int getCommentCountCached(Long postId) {
        return commentRepository.countByPostIdAndParentIsNullAndIsDeletedFalse(postId);

    }

    public int getReplyCountCached(Long parentCommentId) {
        return commentRepository.countByParentIdAndIsDeletedFalse(parentCommentId);
    }


}
