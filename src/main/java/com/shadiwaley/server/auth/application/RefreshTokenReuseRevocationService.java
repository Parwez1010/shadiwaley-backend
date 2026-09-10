package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.infrastructure.repository.UserRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenReuseRevocationService {

    private final UserRefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeActiveTokensForUser(UUID userId) {
        // Commit containment before the caller throws the reuse-detection exception.
        refreshTokenRepository.revokeAllActiveTokensForUser(userId, Instant.now());
    }
}
