package com.shadiwaley.server.engagement.infrastructure.entity;

import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "engagement_loop_state")
public class EngagementLoopState {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak;

    @Column(name = "momentum_score", nullable = false)
    private Integer momentumScore;

    @Column(name = "engagement_score", nullable = false)
    private Integer engagementScore;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "profile_views", nullable = false)
    private Integer profileViews;

    @Column(name = "profile_shares", nullable = false)
    private Integer profileShares;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "profile_live", nullable = false)
    private boolean profileLive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();

        if (currentStreak == null) currentStreak = 0;
        if (momentumScore == null) momentumScore = 0;
        if (engagementScore == null) engagementScore = 0;

        if (profileViews == null) profileViews = 0;
        if (profileShares == null) profileShares = 0;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}