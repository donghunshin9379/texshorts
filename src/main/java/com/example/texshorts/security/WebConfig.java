package com.example.texshorts.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL 패턴 /uploads/** 요청이 로컬 디렉토리 C:/Texshorts/uploads/** 로 매핑됨
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/Texshorts/uploads/");
    }
}