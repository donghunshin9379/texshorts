package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tag_hub")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagHub { //posts tag -> TagHub -> UserInterestTag

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tagName;  // "#서울", "#도쿄" 등

    @Column(nullable = false)
    private Long usageCount = 0L;  // 태그가 사용된 횟수 (default 0)

    @OneToMany(mappedBy = "tagHub", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserInterestTag> userInterestTags = new HashSet<>();

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void decrementUsageCount() {
        if (this.usageCount > 0) { //음수 방어
            this.usageCount--;
        }
    }

//    public void addUserInterestTag(UserInterestTag userInterestTag) {
//        userInterestTags.add(userInterestTag);
//        userInterestTag.setTagHub(this);
//    }
//
//    public void removeUserInterestTag(UserInterestTag userInterestTag) {
//        userInterestTags.remove(userInterestTag);
//        userInterestTag.setTagHub(null);
//    }
}
