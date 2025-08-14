//package com.example.texshorts.config;
//
//import com.example.texshorts.entity.Post;
//import com.example.texshorts.entity.PostTag;
//import com.example.texshorts.entity.TagHub;
//import com.example.texshorts.entity.User;
//import com.example.texshorts.repository.PostRepository;
//import com.example.texshorts.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//@Component
//@RequiredArgsConstructor
//public class SampleDataLoader implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final PostRepository postRepository;
//    private final TagRepository tagRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//        if (postRepository.count() > 0) return;
//
//        // 1. 샘플 유저
//        User user = User.builder()
//                .nickname("sampleUser")
//                .email("sample@example.com")
//                .password("password")
//                .build();
//        userRepository.save(user);
//
//        // 2. 샘플 태그
//        List<String> tagNames = List.of("fun", "tech", "music", "life", "news");
//        Set<TagHub> tagHubs = new HashSet<>();
//        for (String name : tagNames) {
//            TagHub tagHub = TagHub.builder().tagName(name).usageCount(0L).build();
//            tagRepository.save(tagHub);
//            tagHubs.add(tagHub);
//        }
//
//        // 3. 게시물 20개
//        for (int i = 1; i <= 20; i++) {
//            Post post = Post.builder()
//                    .user(user)
//                    .title("Sample Post " + i)
//                    .content("This is the content of sample post number " + i + ".")
//                    .thumbnailPath("sample" + i + ".png")
//                    .visibility("전체 공개")
//                    .createdAt(LocalDateTime.now().minusDays(20 - i))
//                    .tags(String.join(",", tagNames))
//                    .build();
//
//            List<PostTag> postTags = tagHubs.stream()
//                    .map(tagHub -> PostTag.builder().post(post).tagHub(tagHub).build())
//                    .toList();
//            post.setPostTags(postTags);
//
//            postRepository.save(post);
//        }
//
//        System.out.println("✅ 샘플 데이터 20개 생성 완료!");
//    }
//}
