package com.shadiwaley.server.preferences.infrastructure.entity;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfile userProfile;

    @Column(name = "preferred_maslak", length = 50)
    private String preferredMaslak;

    @Column(name = "preferred_state", length = 100)
    private String preferredState;

    @Column(name = "preferred_district", length = 100)
    private String preferredDistrict;

    @Column(name = "min_age")
    private Short minAge;

    @Column(name = "max_age")
    private Short maxAge;

    @Column(name = "preferred_education", length = 50)
    private String preferredEducation;

    @Column(name = "preferred_family_type", length = 50)
    private String preferredFamilyType;

    @Column(name = "require_imam_ref", nullable = false)
    private boolean requireImamRef;

    @Column(name = "require_id_verified", nullable = false)
    private boolean requireIdVerified;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}