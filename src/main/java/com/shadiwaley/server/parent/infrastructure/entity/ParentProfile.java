package com.shadiwaley.server.parent.infrastructure.entity;

import com.shadiwaley.server.parent.domain.ParentRelation;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "parent_profile")
public class ParentProfile {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "parent_name", length = 150)
    private String parentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_relation", length = 40)
    private ParentRelation parentRelation;

    @Column(name = "parent_phone", length = 15)
    private String parentPhone;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state;

    @Column(length = 50)
    private String maslak;

    @Column(name = "imam_reference", length = 200)
    private String imamReference;

    @Column(name = "masjid_name", length = 200)
    private String masjidName;

    @Column(name = "consent_recorded_at")
    private Instant consentRecordedAt;

    @Column(name = "consent_type", length = 30)
    private String consentType;

    @Column(length = 100)
    private String caste;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}