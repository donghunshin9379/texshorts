package com.example.texshorts.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private Key key;
    private final long validityInMilliseconds = 3600000; // 1시간

    @Value("${jwt.secret}")
    private String secretKey;


    // secretKey를 Key 객체로 초기화
    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 생성
    public String createToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256) // Key 객체 사용
                .compact();
    }

    // 토큰에서 사용자 이름 추출
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰에서 권한 리스트 추출
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 권한 리스트를 GrantedAuthority 리스트로 변환
    public List<SimpleGrantedAuthority> createAuthorities(List<String> roles) {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // Authentication 객체로부터 토큰 생성 (Flutter 로그인 응답용)
    public String generateToken(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();

        List<String> roles = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());

        return createToken(username, roles);
    }

}
