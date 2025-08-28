package com.example.texshorts.component;

import com.example.texshorts.dto.CommentResponseDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotifier.class);

    // 댓글 생성 (완료)호출  구독요청XXXXXXXXX
    public void notifyNewComment(Long postId, CommentResponseDTO dto) {
        String destination = "/subscribe/post/" + postId + "/comments";
        messagingTemplate.convertAndSend(destination, dto);
        logger.info("💬 댓글생성완료알림 구독경로: {}", destination);
        logger.info("웹소켓 호출 메소드 실행 dto :{} ", dto);
    }
    
}
