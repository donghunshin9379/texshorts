package com.example.texshorts.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /thumbnails/** 요청을 C:/Texshorts/uploads/thumbnails 폴더로 연결
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:///" + uploadDir + "/thumbnails/");
    }
}