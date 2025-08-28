package com.example.texshorts.component;

import com.example.texshorts.dto.CommentResponseDTO;
import com.example.texshorts.dto.message.CommentCreationMessage;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Component
@RequiredArgsConstructor
public class CommentCreationService  {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;
    private final WebSocketNotifier webSocketNotifier;

    private static final Logger logger = LoggerFactory.getLogger(CommentCreationService .class);

    @Transactional
    public CommentResponseDTO createCommentFromMessage(CommentCreationMessage msg) {
        // 1. 게시물 조회
        Post post = postRepository.findById(msg.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        // 2. 사용자 조회
        User user = userRepository.findById(msg.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 댓글 생성 & 저장
        Comment comment = Comment.createRootComment(post, user, msg.getContent());
        commentRepository.save(comment);

        // 4. CommentResponseDTO 생성 (수동)
        CommentResponseDTO dto = new CommentResponseDTO(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getCreatedAt()
        );

        dto.setLikeCount(0);
        dto.setReplyCount(0);
        dto.setIsDeleted(false);
        // 5. 캐시 append
        redisCacheService.appendRootComment(msg.getPostId(), dto);
        redisCacheService.incrementRootCommentCount(msg.getPostId());

        // 캐시 확인
        logger.info("CommentCreationService 캐시 append 후 댓글 리스트 : {}",redisCacheService.getRootCommentList(msg.getPostId()));
        // 트랜잭션 커밋 후에 알림
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketNotifier.notifyNewComment(msg.getPostId(), dto);
            }
        });

        return dto;
    }




}