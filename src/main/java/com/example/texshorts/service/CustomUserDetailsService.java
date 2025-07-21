package com.example.texshorts.service;

import com.example.texshorts.custom.CustomUserDetails;
import com.example.texshorts.entity.User;
import com.example.texshorts.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class CustomUserDetailsService implements UserDetailsService {
    //실제 User 엔티티 조회

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username) //로그인 아이디.
                .orElseThrow(() -> new UsernameNotFoundException("사용자가 존재하지 않습니다"));
        logger.info("로그인 :{}", user.getUsername());
        logger.info("계정권한 :{}", user.getRoles());

        // CustomUserDetails 객체 생성 후 반환
        return new CustomUserDetails(user);
    }



}
