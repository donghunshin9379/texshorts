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

    /** 루트 댓글 조회 */
    // 특정 게시물의 전체댓글을 가져오고 캐시 또한 전체저장(물론 TTL)
    public CommentListResponseDTO getRootComments(Long postId) {
        List<CommentResponseDTO> dtoList = commentRepository.findRootCommentDTOs(postId);
        dtoList.forEach(dto -> dto.setReplies(List.of()));
        CommentListResponseDTO responseDTO = new CommentListResponseDTO(dtoList);
        // 캐시에 DTO 전체 저장 (lastCommentId 포함)
        redisCacheService.cacheRootComments(postId, responseDTO);
        return responseDTO;
    }

    /** 답글 목록 조회 */
    public ReplyCommentListResponseDTO getReplies(Long parentCommentId) {
        List<CommentResponseDTO> dtoList = commentRepository.findReplyDTOs(parentCommentId);
        dtoList.forEach(dto -> dto.setReplies(List.of()));
        // 캐시 저장(새로고침 대응)
        redisCacheService.cacheReplies(parentCommentId, dtoList);
        return new ReplyCommentListResponseDTO(dtoList);
    }

    /** [댓글 새로고침] 기존댓글(캐시) + 최신댓글 */
    public List<CommentResponseDTO> refreshRootComments(Long postId, Long lastCommentId) {
        CommentListResponseDTO cachedDTO = redisCacheService.getCachedRootCommentsDTO(postId);
        List<CommentResponseDTO> cached = cachedDTO != null ? cachedDTO.getComments() : getRootComments(postId).getComments();

        List<CommentResponseDTO> newComments = commentRepository.findRootCommentsAfter(postId, lastCommentId);
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
