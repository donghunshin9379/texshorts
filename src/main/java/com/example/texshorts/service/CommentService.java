package com.example.texshorts.service;

import com.example.texshorts.dto.CommentListResponseDTO;
import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.ReplyCommentListResponseDTO;
import com.example.texshorts.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RedisCacheService redisCacheService;

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    /** 루트 댓글 조회 (DB 기반) */
    // 댓글 목록 조회
    public List<CommentResponseDTO> getComments(Long postId, Long lastCommentId) {
        if (lastCommentId == null) {
            // 처음 조회 → 전체 조회 + 캐시
            List<CommentResponseDTO> cached = redisCacheService.getRootCommentList(postId);
            if (!cached.isEmpty()) return cached;

            List<CommentResponseDTO> dtos = commentRepository.findRootCommentDTOs(postId);
            redisCacheService.cacheRootComments(postId, dtos);
            return dtos;
        } else {
            // 이후 조회 → lastCommentId 이후만 가져오기 (증분)
            return commentRepository.findRootCommentsAfter(postId, lastCommentId);
        }
    }

    /** 답글 목록 조회 */
    public ReplyCommentListResponseDTO getReplies(Long parentCommentId) {
        List<CommentResponseDTO> dtoList = commentRepository.findReplyDTOs(parentCommentId);
        dtoList.forEach(dto -> dto.setReplies(List.of()));
        // 캐시 저장(새로고침 대응)
        redisCacheService.cacheReplies(parentCommentId, dtoList);
        return new ReplyCommentListResponseDTO(dtoList);
    }

    /** 댓글 수동 새로고침  */
    public List<CommentResponseDTO> refreshRootComments(Long postId, Long lastCommentId) {
        List<CommentResponseDTO> cached = redisCacheService.getRootCommentList(postId);

        List<CommentResponseDTO> newComments = commentRepository.findRootCommentsAfter(postId, lastCommentId);
        newComments.forEach(dto -> redisCacheService.appendRootComment(postId, dto));

        List<CommentResponseDTO> combined = new ArrayList<>(cached);
        combined.addAll(newComments);
        return combined;
    }

    /** [답글 새로고침] 기존댓글(캐시) + 최신댓글*/
    public List<CommentResponseDTO> refreshReplies(Long parentCommentId, Long lastReplyId) {
        List<CommentResponseDTO> cached = redisCacheService.getCachedReplies(parentCommentId);
        if (cached == null) {
            cached = getReplies(parentCommentId).getReplies();
        }
        List<CommentResponseDTO> newReplies = commentRepository.findRepliesAfter(parentCommentId, lastReplyId);
        List<CommentResponseDTO> combined = new ArrayList<>(cached);
        combined.addAll(newReplies);
        return combined;
    }

    public int getCommentCountCached(Long postId) {
        return redisCacheService.getRootCommentCount(postId);
    }

    public int getReplyCountCached(Long parentCommentId) {
        return redisCacheService.getReplyCount(parentCommentId);
    }


}
