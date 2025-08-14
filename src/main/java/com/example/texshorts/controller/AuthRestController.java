package com.example.texshorts.controller;

import com.example.texshorts.dto.JwtResponse;
import com.example.texshorts.dto.LoginRequest;
import com.example.texshorts.dto.SignupRequest;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.config.JwtTokenProvider;
import com.example.texshorts.service.SignUpService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final SignUpService signupService;
    private static final Logger logger = LoggerFactory.getLogger(AuthRestController.class);



    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            return ResponseEntity.badRequest().body("이미 존재하는 닉네임입니다.");
        }

        if (signupRequest.getNickname().length() > 10) {
            return ResponseEntity.badRequest().body("닉네임은 10자리 미만이어야 합니다.");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body("이미 존재하는 이메일입니다.");
        }

        User user = signupService.createUserFromSignup(signupRequest);
        userRepository.save(user);

        return ResponseEntity.ok("회원가입 성공");
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()
                    )
            );

            String token = jwtTokenProvider.generateToken(authentication);
            List<String> roles = authentication.getAuthorities().stream()
                    .map(r -> r.getAuthority())
                    .toList();

            return ResponseEntity.ok(new JwtResponse(token, roles));

        } catch (AuthenticationException e) {
            logger.error("로그인 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken() {
        return ResponseEntity.ok().build();
    }



}
