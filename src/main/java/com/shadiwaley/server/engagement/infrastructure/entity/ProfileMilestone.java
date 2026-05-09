package com.shadiwaley.server.engagement.infrastructure.entity;

import com.shadiwaley.server.engagement.domain.MilestoneCode;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "profile_milestone")
public class ProfileMilestone {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_code", nullable = false, length = 80)
    private MilestoneCode milestoneCode;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean achieved;

    @Column(name = "achieved_at")
    private Instant achievedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}