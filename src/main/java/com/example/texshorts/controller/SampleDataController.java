package com.example.texshorts.controller;

import com.example.texshorts.component.PostCreationService;
import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.dto.PostCreateRequest;
import com.example.texshorts.dto.message.PostCreationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class SampleDataController {

    private final PostCreationService postCreationService;

    private static final List<String> TAG_POOL = List.of(
            "#성장", "#경험", "#도전", "#여행", "#음악",
            "#기술", "#코딩", "#운동", "#맛집", "#독서"
    );
    private static final Random RANDOM = new Random();

    private static final List<String> CONTENT_POOL = List.of(
            "오늘은 날씨가 정말 좋았다.",
            "새로운 기술을 배우는 것은 즐거운 일이다.",
            "여행을 떠나고 싶은 마음이 가득하다.",
            "맛있는 음식을 먹으면 기분이 좋아진다.",
            "운동을 하면 스트레스가 풀린다.",
            "책을 읽는 시간은 나만의 힐링 시간이다.",
            "친구와 함께한 시간이 소중하게 느껴진다.",
            "작은 도전이 큰 변화를 만들어낸다.",
            "음악을 들으며 마음을 가다듬는다.",
            "오늘도 성장하기 위해 노력했다.",
            "자연 속에서 보내는 시간은 마음을 편안하게 한다.",
            "새로운 사람과의 만남은 항상 설레임을 준다."
    );

    @PostMapping("/load-sample-data")
    public String loadSampleData(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();

        for (int i = 1; i <= 20; i++) {
            // 랜덤 태그 선택
            int tagCount = 2 + RANDOM.nextInt(3);
            Set<String> selectedTags = new HashSet<>();
            while (selectedTags.size() < tagCount) {
                selectedTags.add(TAG_POOL.get(RANDOM.nextInt(TAG_POOL.size())));
            }
            String tagsString = String.join(" ", selectedTags);

            // 랜덤 content (8~12문장)
            int sentenceCount = 8 + RANDOM.nextInt(5);
            StringBuilder contentBuilder = new StringBuilder();
            for (int j = 0; j < sentenceCount; j++) {
                contentBuilder.append(CONTENT_POOL.get(RANDOM.nextInt(CONTENT_POOL.size()))).append(" ");
            }
            String contentString = contentBuilder.toString().trim();

            // 랜덤 썸네일 파일명 (DB에는 파일명만 저장)
            int thumbnailIndex = 1 + RANDOM.nextInt(20);
            String thumbnailFileName = "sample" + thumbnailIndex + ".jpg"; // uploads/thumbnails 폴더에 존재

            PostCreateRequest dto = PostCreateRequest.builder()
                    .title("Sample Post " + i)
                    .content(contentString)
                    .tags(tagsString)
                    .location("Seoul")
                    .visibility("전체 공개")
                    .build();

            PostCreationMessage msg = PostCreationMessage.builder()
                    .userId(userId)
                    .thumbnailPath(thumbnailFileName) // 절대 URL 제거
                    .postCreateRequest(dto)
                    .build();

            postCreationService.createPostFromMessage(msg);
        }

        return "샘플 데이터 생성 완료!";
    }
}
