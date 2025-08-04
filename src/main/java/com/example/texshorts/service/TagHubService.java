package com.example.texshorts.service;

import com.example.texshorts.entity.Post;
import com.example.texshorts.entity.TagHub;
import com.example.texshorts.repository.TagHubRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**태그 등록 및 사용량 추적 서비스*/
@Service
@RequiredArgsConstructor
public class TagHubService {
    private final TagHubRepository tagHubRepository;
    private static final Logger logger = LoggerFactory.getLogger(TagHub.class);
    private final TagParserUtils tagParserUtils;


    // 태그 사용 증가 처리 (생성 OR 스택)
    @Transactional
    public void registerOrUpdateTagUsage(List<String> tagNames) {
        logger.info("레지스터태그 메소드 실행 태그네임들 :{}", tagNames);
        for (String tagName : tagNames) {
            Optional<TagHub> optionalTagHub = tagHubRepository.findByTagName(tagName);
            if (optionalTagHub.isPresent()) {
                TagHub tagHub = optionalTagHub.get();
                tagHub.incrementUsageCount();
            } else {
                TagHub newTag = TagHub.builder()
                        .tagName(tagName)
                        .usageCount(1L)
                        .build();
                tagHubRepository.save(newTag);
            }
        }
    }

//
//    // 태그 사용량 감소 처리
//    @Transactional
//    public void decreaseTagUsageFromPost(Post post) {
//        List<String> tagNames = tagParserUtils.parseTagsToList(post.getTags());  // 문자열 → 리스트 변환
//
//        for (String tagName : tagNames) {
//            Optional<TagHub> optionalTagHub = tagHubRepository.findByTagName(tagName);
//
//            if (optionalTagHub.isPresent()) {
//                TagHub tagHub = optionalTagHub.get();
//                tagHub.decrementUsageCount();
//
//                if (tagHub.getUsageCount() <= 0) { //음수 방지
//                    tagHubRepository.delete(tagHub);
//                }
//            }
//        }
//    }




}
