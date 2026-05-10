package com.shadiwaley.server.safety.infrastructure.entity;

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
        name = "user_block",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_block_pair",
                        columnNames = {"blocker_user_id", "blocked_user_id"}
                )
        }
)
public class UserBlock {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id", nullable = false)
    private UserAccount blockerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private UserAccount blockedUser;

    @Column(length = 300)
    private String reason;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}