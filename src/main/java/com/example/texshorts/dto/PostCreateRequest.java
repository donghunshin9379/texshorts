package com.example.texshorts.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PostCreateRequest { /** 게시물 생성용 바디 DTO*/
    private String title;
    private String content;
    private String tags;
    private String location;
    private String visibility;

}
