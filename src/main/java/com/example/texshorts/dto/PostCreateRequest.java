package com.example.texshorts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest { /** 게시물 생성용 바디 DTO*/
    private String title;
    private String content;
    private String tags;
    private String location;
    private String visibility;

}
