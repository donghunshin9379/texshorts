package com.example.texshorts.component;

import com.example.texshorts.dto.message.ReplyCommentCreationMessage;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReplyCommentCreationService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ReplyCommentCreationService.class);

    @Transactional
    public void createReplyCommentFromMessage(ReplyCommentCreationMessage msg) {
        // 1. 부모 댓글 조회
        Comment parentComment = commentRepository.findById(msg.getParentCommentId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 댓글입니다."));

        // 2. 작성자(User) 조회
        User user = userRepository.findById(msg.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 답글 생성
        Comment reply = Comment.createReply(parentComment, user, msg.getContent());

        // 4. DB 저장
        commentRepository.save(reply);

        // 필요시 부모 댓글의 replyCount 동기화 로직 추가 가능
        parentComment.setReplyCount(parentComment.getReplyCount() + 1);
        commentRepository.save(parentComment);

        logger.info("대댓글 생성 완료: parentCommentId={}, userId={}", parentComment.getId(), user.getId());
    }
}
