package com.example.texshorts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor // 모든 필드를 받는 생성자 생성
public class JwtResponse {
    private String token;
    private List<String> roles;
}
