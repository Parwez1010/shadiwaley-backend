package com.shadiwaley.server.auth.infrastructure.entity;

import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "user_refresh_token",
        indexes = {
                @Index(name = "idx_user_refresh_token_user", columnList = "user_account_id"),
                @Index(name = "idx_user_refresh_token_expires", columnList = "expires_at"),
                @Index(name = "idx_user_refresh_token_revoked", columnList = "revoked_at")
        }
)
public class UserRefreshToken {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(name = "token_hash", nullable = false, length = 500)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isActive() {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(Instant.now());
    }
}