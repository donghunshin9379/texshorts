package com.example.texshorts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final RedisTemplate<String, String> redisTemplate;

    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("Redis get 실패: " + e.getMessage());
            return null;
        }
    }

    public void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            System.err.println("Redis set 실패: " + e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Redis delete 실패: " + e.getMessage());
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            System.err.println("Redis increment 실패: " + e.getMessage());
            return null;
        }
    }

    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            System.err.println("Redis decrement 실패: " + e.getMessage());
            return null;
        }
    }
}