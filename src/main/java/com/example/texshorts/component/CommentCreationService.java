package com.example.texshorts.component;

import com.example.texshorts.dto.message.CommentCreationMessage;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CommentCreationService  {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CommentCreationService .class);

    @Transactional
    public void createCommentFromMessage(CommentCreationMessage msg) {
        // 1. 게시물 조회
        Post post = postRepository.findById (msg.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        // 2. 사용자 조회
        User user = userRepository.findById(msg.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 댓글 생성 & 저장
        Comment comment = Comment.createRootComment(post, user, msg.getContent());
        commentRepository.save(comment);

    }

}