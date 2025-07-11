package com.example.texshorts.service;

import com.example.texshorts.DTO.SignupRequest;
import com.example.texshorts.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final PasswordEncoder passwordEncoder;

    public User createUserFromSignup(SignupRequest request) {
        return User.builder()
                .username(request.getUsername())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .roles(List.of("ROLE_USER"))
                .build();
    }


}
