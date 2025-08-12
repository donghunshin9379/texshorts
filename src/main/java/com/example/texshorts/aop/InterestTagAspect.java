package com.example.texshorts.aop;

import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.service.InterestTagQueueService;
import com.example.texshorts.service.RedisCacheService;
import com.example.texshorts.service.ViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class InterestTagAspect {

    private final InterestTagQueueService interestTagQueueService;
    private final RedisCacheService redisCacheService;

    @Around("execution(* com.example.texshorts.service.ViewService.increaseViewCountIfNotViewed(..)) || " +
            "execution(* com.example.texshorts.service.CommentService.createRootComment(..)) || " +
            "execution(* com.example.texshorts.service.PostReactionService.react(..)) || " +
            "execution(* com.example.texshorts.service.CommentReactionService.react(..))")
    public Object aroundTargetMethods(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        Object[] args = pjp.getArgs();

        Long userId = null;
        Long postId = null;
        boolean isNotDuplicate = false;
        ReactionType reactionType = null;

        switch (methodName) {
            case "increaseViewCountIfNotViewed" -> {
                if (args.length == 2 && args[0] instanceof Long && args[1] instanceof Long) {
                    postId = (Long) args[0];
                    userId = (Long) args[1];
                    if (redisCacheService.hasViewed(userId, postId)) {
                        log.info("[AOP] 중복 시청 detected, 메서드 실행 차단 - userId: {}, postId: {}", userId, postId);
                        return null;
                    }
                    isNotDuplicate = true;
                }
            }

            case "createRootComment" -> {
                if (args.length == 3 && args[0] instanceof Long && args[1] instanceof User) {
                    postId = (Long) args[0];
                    userId = ((User) args[1]).getId();
                }
            }

            case "react" -> {
                if (args.length == 3 && args[2] instanceof ReactionType type) {
                    reactionType = type;
                    if (args[0] instanceof Long l) postId = l;
                    if (args[1] instanceof User u) userId = u.getId();
                }
            }

            default -> {
                log.warn("[AOP] 처리되지 않은 메서드입니다: {}", methodName);
                return pjp.proceed();
            }
        }

        // 원래 메서드 실행
        Object result = pjp.proceed();

        // 관심태그 갱신 처리
        if (userId != null && postId != null) {
            if ("increaseViewCountIfNotViewed".equals(methodName) && isNotDuplicate) {
                interestTagQueueService.enqueueAddInterestTagsFromPost(userId, postId);
                log.info("[AOP] 조회로 인한 관심태그 추가 - userId: {}, postId: {}", userId, postId);
            } else if ("createRootComment".equals(methodName)) {
                interestTagQueueService.enqueueAddInterestTagsFromPost(userId, postId);
                log.info("[AOP] 댓글로 인한 관심태그 추가 - userId: {}, postId: {}", userId, postId);
            } else if ("react".equals(methodName) && reactionType != null) {
                if (reactionType == ReactionType.LIKE) {
                    interestTagQueueService.enqueueAddInterestTagsFromPost(userId, postId);
                    log.info("[AOP] 좋아요로 인한 관심태그 추가 - userId: {}, postId: {}", userId, postId);
                } else if (reactionType == ReactionType.DISLIKE) {
                    interestTagQueueService.enqueueRemoveInterestTagsFromPost(userId, postId);
                    log.info("[AOP] 싫어요로 인한 관심태그 삭제 - userId: {}, postId: {}", userId, postId);
                }
            }
        }

        return result;
    }
}
