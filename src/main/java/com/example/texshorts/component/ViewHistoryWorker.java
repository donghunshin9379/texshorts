package com.example.texshorts.component;

import com.example.texshorts.entity.ViewHistory;
import com.example.texshorts.repository.PostRepository;
import com.example.texshorts.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ViewHistoryWorker {

    private final ViewHistoryRepository viewHistoryRepository;
    private final PostRepository postRepository;


    private final Logger logger = LoggerFactory.getLogger(ViewHistoryWorker.class);

    @Transactional
    public void saveViewHistory(Long userId, Long postId) {
        boolean exists = viewHistoryRepository.existsByUserIdAndPostId(userId, postId);
        if (exists) {
            logger.info("이미 저장된 조회 기록 존재 - userId: {}, postId: {}", userId, postId);
            return;
        }

        ViewHistory vh = new ViewHistory();
        vh.setUserId(userId);
        vh.setPost(postRepository.getReferenceById(postId));
        vh.setViewedAt(LocalDateTime.now());

        viewHistoryRepository.save(vh);
        logger.info("조회 기록 저장 완료 - userId: {}, postId: {}", userId, postId);

    }

}
