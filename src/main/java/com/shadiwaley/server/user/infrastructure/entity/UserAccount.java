package com.shadiwaley.server.user.infrastructure.entity;

import com.shadiwaley.server.user.domain.UserRole;
import com.shadiwaley.server.user.domain.UserSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "account_status", nullable = false, length = 30)
    private String accountStatus;

    @Column(name = "refresh_token_hash", length = 500)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (role == null) role = UserRole.USER;
        if (accountStatus == null) accountStatus = "ACTIVE";

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}