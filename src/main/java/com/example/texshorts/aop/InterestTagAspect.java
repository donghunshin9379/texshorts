package com.example.texshorts.aop;

import com.example.texshorts.entity.ReactionType;
import com.example.texshorts.entity.User;
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
            "execution(* com.example.texshorts.service.CommentService.createReply(..)) || " +
            "execution(* com.example.texshorts.service.PostReactionService.react(..)) || " +
            "execution(* com.example.texshorts.service.CommentReactionService.react(..))")
    public void afterTargetMethods(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[AOP] 메소드 실행 완료: {}", methodName);

        Long userId = null;
        Long postId = null;

        /**ReactionType 필터링*/
        if (methodName.equals("react") && args.length == 3 && args[2] instanceof ReactionType) {
            ReactionType type = (ReactionType) args[2];

            /** 좋아요일 때만 큐 등록*/
            if (type != ReactionType.LIKE) {
                log.info("[AOP] 싫어요이므로 관심태그 큐 등록 생략 - type: {}", type);
                return;
            }

            // postId, user 추출
            if (args[0] instanceof Long) postId = (Long) args[0];
            if (args[1] instanceof User) userId = ((User) args[1]).getId();
        }

        if (methodName.equals("increaseViewCountIfNotViewed") && args.length == 2) {
            if (args[0] instanceof Long && args[1] instanceof Long) {
                postId = (Long) args[0];
                userId = (Long) args[1];
            }
        } else if (methodName.equals("createRootComment") && args.length == 3) {
            if (args[0] instanceof Long && args[1] instanceof User) {
                postId = (Long) args[0];
                userId = ((User) args[1]).getId();
            }
        } else if (methodName.equals("createReply") && args.length >= 2 && args[0] instanceof Long && args[1] instanceof User) {
            log.info("[AOP] createReply는 postId 추출 불가, 관심태그 큐 등록 생략");
            return;
        }

        if (userId != null && postId != null) {
            viewService.enqueueAddInterestTagsFromPost(userId, postId);
            log.info("[AOP] 관심태그 큐 등록 요청 - userId: {}, postId: {}", userId, postId);
        } else {
            log.warn("[AOP] userId 또는 postId 파라미터를 찾지 못했습니다.");
        }
    }
}

