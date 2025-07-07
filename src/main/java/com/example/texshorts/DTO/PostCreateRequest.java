package com.example.texshorts.DTO;

import lombok.Data;

@Data
public class PostCreateRequest {
    private String title;
    private String content;
    private String tags;
    private String location;
    private String visibility;

}
