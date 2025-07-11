package com.example.texshorts.controller;

import com.example.texshorts.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/signup")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 아이디 중복 체크 API
    @GetMapping("/check-username")
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean available = !userRepository.existsByUsername(username);
        //JSON 반환
        return Collections.singletonMap("available", available);
    }

    // 닉네임 중복 체크 API
    @GetMapping("/check-nickname")
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = !userRepository.existsByNickname(nickname);
        return Collections.singletonMap("available", available);
    }

    // 이메일 중복 체크 API
    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean available = !userRepository.existsByEmail(email);
        return Collections.singletonMap("available", available);
    }

}
