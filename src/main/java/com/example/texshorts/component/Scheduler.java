package com.example.texshorts.component;

import com.example.texshorts.service.RequestRedisQueue;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private final RequestRedisQueue requestRedisQueue;
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Scheduled(fixedRate = 30 * 60 * 1000) // 30분 주기
    public void schedulePopularFeedRefresh() {
        requestRedisQueue.enqueuePopularFeedRefresh();
        logger.info("인기 피드 스케줄러 실행(30분주기)");
    }

}
