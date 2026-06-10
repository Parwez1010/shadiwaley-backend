package com.shadiwaley.server.customer.savedprofile.entity;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
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
        name = "saved_profile",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saved_profile_user_profile",
                        columnNames = {"user_account_id", "saved_profile_id"}
                )
        },
        indexes = {
                @Index(name = "idx_saved_profile_user", columnList = "user_account_id"),
                @Index(name = "idx_saved_profile_profile", columnList = "saved_profile_id")
        }
)
public class SavedProfile {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_profile_id", nullable = false)
    private UserProfile savedProfile;

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
}