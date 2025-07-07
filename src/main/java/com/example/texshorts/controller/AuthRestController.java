package com.example.texshorts.controller;

import com.example.texshorts.DTO.JwtResponse;
import com.example.texshorts.DTO.LoginRequest;
import com.example.texshorts.DTO.SignupRequest;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.UserRepository;
import com.example.texshorts.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthRestController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                              UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setNickname(signupRequest.getNickname());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setEmail(signupRequest.getEmail());
        user.setGender(signupRequest.getGender());
        user.setBirthDate(signupRequest.getBirthDate());
        user.setRoles(List.of("ROLE_USER"));

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }


}
