package com.example.texshorts.component;

import com.example.texshorts.dto.message.PostDeleteMessage;
import com.example.texshorts.entity.Comment;
import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.ViewHistory;
import com.example.texshorts.repository.CommentRepository;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.PostViewRepository;
import com.example.texshorts.repository.ViewHistoryRepository;
import com.example.texshorts.service.PostTagService;
import com.example.texshorts.service.TagHubService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostDeletionService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final PostTagService postTagService;
    private final PostViewRepository postViewRepository;


    private static final Logger logger = LoggerFactory.getLogger(PostDeletionService.class);

    public void deletePostHard(PostDeleteMessage msg) {
        Long postId = msg.getPostId();
        Long userId = msg.getUserId();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시물이 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 게시물이 아닙니다.");
        }

        // 댓글, 조회수, 태그 등 배치 삭제
        deleteCommentsInBatches(postId);
        deleteViewHistoriesInBatches(postId);
        postTagService.unlinkPostAndTags(post);

        postRepository.deleteById(postId);
        postViewRepository.deleteByPostId(postId);

        logger.info("게시물 삭제 완료: postId={}", postId);
    }

    private void deleteCommentsInBatches(Long postId) {
        while (true) {
            List<Comment> batch = commentRepository.findTopNByPostId(postId, 500);
            if (batch.isEmpty()) break;
            commentRepository.deleteAll(batch);
        }
    }

    private void deleteViewHistoriesInBatches(Long postId) {
        while (true) {
            List<ViewHistory> batch = viewHistoryRepository.findTopNByPostId(postId, 500);
            if (batch.isEmpty()) break;
            viewHistoryRepository.deleteAll(batch);
        }
    }

}