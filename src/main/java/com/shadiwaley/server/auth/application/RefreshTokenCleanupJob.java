package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.infrastructure.repository.UserRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final UserRefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoff = Instant.now().minusSeconds(7L * 24 * 60 * 60);

        refreshTokenRepository.deleteExpiredBefore(cutoff);
    }
}