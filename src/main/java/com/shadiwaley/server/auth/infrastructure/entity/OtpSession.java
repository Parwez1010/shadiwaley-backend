package com.shadiwaley.server.auth.infrastructure.entity;

import com.shadiwaley.server.user.domain.UserSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "otp_session")
public class OtpSession {

    @Id
    private UUID id;

    @Column(name = "temp_token", nullable = false, unique = true)
    private UUID tempToken;

    @Column(nullable = false, length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserSide side;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (tempToken == null) tempToken = UUID.randomUUID();
        createdAt = Instant.now();
    }
}