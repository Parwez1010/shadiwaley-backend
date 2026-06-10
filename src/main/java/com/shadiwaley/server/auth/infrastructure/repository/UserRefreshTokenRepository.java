package com.shadiwaley.server.auth.infrastructure.repository;

import com.shadiwaley.server.auth.infrastructure.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, UUID> {

    Optional<UserRefreshToken> findById(UUID id);

    @Modifying
    @Query("""
            UPDATE UserRefreshToken t
            SET t.revokedAt = :now
            WHERE t.userAccount.id = :userId
            AND t.revokedAt IS NULL
            """)
    int revokeAllActiveTokensForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            DELETE FROM UserRefreshToken t
            WHERE t.expiresAt < :before
            """)
    int deleteExpiredBefore(@Param("before") Instant before);
}