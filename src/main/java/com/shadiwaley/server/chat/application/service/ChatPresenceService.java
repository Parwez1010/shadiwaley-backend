package com.shadiwaley.server.chat.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_PREFIX = "chat:online:";
    private static final Duration TTL = Duration.ofMinutes(2);

    public void markOnline(UUID userId) {
        redisTemplate.opsForValue().set(
                ONLINE_PREFIX + userId,
                "1",
                TTL
        );
    }

    public boolean isOnline(UUID userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(ONLINE_PREFIX + userId)
        );
    }

    public void markOffline(UUID userId) {
        redisTemplate.delete(ONLINE_PREFIX + userId);
    }
}