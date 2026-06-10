package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.infrastructure.entity.UserRefreshToken;
import com.shadiwaley.server.auth.infrastructure.repository.UserRefreshTokenRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRefreshTokenRepository refreshTokenRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token-expiry-days:30}")
    private long refreshTokenExpiryDays;

    public String createRefreshToken(
            UserAccount userAccount,
            String deviceInfo,
            String ipAddress
    ) {

        UUID tokenId = UUID.randomUUID();

        String secret = UUID.randomUUID().toString()
                + UUID.randomUUID();

        UserRefreshToken refreshToken = new UserRefreshToken();
        refreshToken.setId(tokenId);
        refreshToken.setUserAccount(userAccount);
        refreshToken.setTokenHash(passwordEncoder.encode(secret));
        refreshToken.setExpiresAt(
                Instant.now()
                        .plusSeconds(refreshTokenExpiryDays * 24 * 60 * 60)
        );
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);

        refreshTokenRepository.save(refreshToken);

        return tokenId + "." + secret;
    }

    public UserRefreshToken validateRefreshToken(String token) {

        String[] parts = token.split("\\.");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UUID tokenId = UUID.fromString(parts[0]);
        String secret = parts[1];

        UserRefreshToken refreshToken =
                refreshTokenRepository
                        .findById(tokenId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Refresh token not found"));

        if (refreshToken.getRevokedAt() != null) {

            revokeAllForUser(
                    refreshToken.getUserAccount().getId()
            );

            throw new IllegalArgumentException(
                    "Refresh token reuse detected. All sessions revoked."
            );
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        if (!passwordEncoder.matches(secret, refreshToken.getTokenHash())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        return refreshToken;
    }
    @Transactional
    public String rotateRefreshToken(
            UserRefreshToken oldToken,
            String deviceInfo,
            String ipAddress
    ) {
        Instant now = Instant.now();

        oldToken.setRevokedAt(now);
        oldToken.setLastUsedAt(now);

        String newToken = createRefreshToken(
                oldToken.getUserAccount(),
                deviceInfo,
                ipAddress
        );

        UUID newTokenId = UUID.fromString(newToken.split("\\.")[0]);
        oldToken.setReplacedByTokenId(newTokenId);

        refreshTokenRepository.save(oldToken);

        return newToken;
    }

    @Transactional
    public void revokeToken(String token) {
        UserRefreshToken refreshToken = validateRefreshToken(token);

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllActiveTokensForUser(
                userId,
                Instant.now()
        );
    }


}