package com.shadiwaley.server.autopilot.infrastructure.entity;

import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "autopilot_preference")
public class AutopilotPreference {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "whatsapp_number", length = 15)
    private String whatsappNumber;

    @Column(name = "photo_consent_level", nullable = false, length = 30)
    private String photoConsentLevel;

    @Column(name = "preferred_dispatch_day", nullable = false, length = 20)
    private String preferredDispatchDay;

    @Column(name = "profiles_per_week", nullable = false)
    private Short profilesPerWeek;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        photoConsentLevel = photoConsentLevel == null ? "AFTER_ACCEPT" : photoConsentLevel;
        preferredDispatchDay = preferredDispatchDay == null ? "TUESDAY" : preferredDispatchDay;
        profilesPerWeek = profilesPerWeek == null ? (short) 3 : profilesPerWeek;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}