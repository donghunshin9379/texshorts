package com.example.texshorts.aop;

import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
import com.example.texshorts.service.UserInterestTagService;
import com.example.texshorts.service.ViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class InterestTagAspect {

    private final ViewService viewService;

    @AfterReturning("execution(* com.example.texshorts.service.ViewService.increaseViewCountIfNotViewed(..)) || " +
            "execution(* com.example.texshorts.service.CommentService.createRootComment(..)) || " +
            "execution(* com.example.texshorts.service.PostReactionService.react(..)) || " +
            "execution(* com.example.texshorts.service.CommentReactionService.react(..))")
    public void afterTargetMethods(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[AOP] 메소드 실행 완료: {}", methodName);

        Long userId = null;
        Long postId = null;

        switch (methodName) {
            case "saveViewHistory" -> {
                if (args.length == 2 && args[0] instanceof Long && args[1] instanceof Long) {
                    userId = (Long) args[0];
                    postId = (Long) args[1];
                }
            }
            case "increaseViewCountIfNotViewed" -> {
                if (args.length == 2 && args[0] instanceof Long && args[1] instanceof Long) {
                    postId = (Long) args[0];
                    userId = (Long) args[1];
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
                    if (type != ReactionType.LIKE) {
                        log.info("[AOP] 싫어요이므로 관심태그 큐 등록 생략 - type: {}", type);
                        return;
                    }
                    if (args[0] instanceof Long l) postId = l;
                    if (args[1] instanceof User u) userId = u.getId();
                }
            }
            default -> {
                log.warn("[AOP] 처리되지 않은 메서드입니다: {}", methodName);
                return;
            }
        }

        if (userId != null && postId != null) {
            // 실제 관심태그 추가하는 메서드 호출 말고, 큐에 메시지만 넣기
            viewService.enqueueAddInterestTagsFromPost(userId, postId);
            log.info("[AOP] 관심태그 큐 등록 요청 - userId: {}, postId: {}", userId, postId);
        } else {
            log.warn("[AOP] userId 또는 postId 파라미터를 찾지 못했습니다.");
        }
    }

}


