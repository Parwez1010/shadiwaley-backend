package com.shadiwaley.server.profile.infrastructure.entity;

import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "display_id", length = 20, unique = true)
    private String displayId;

    @Column(name = "candidate_first_name", length = 100)
    private String candidateFirstName;

    @Column(name = "candidate_age")
    private Short candidateAge;

    @Column(name = "candidate_height_cm")
    private Short candidateHeightCm;

    @Column(length = 50)
    private String education;

    @Column(name = "quran_level", length = 50)
    private String quranLevel;

    @Column(name = "namaaz_regularity", length = 50)
    private String namaazRegularity;

    @Column(name = "previously_married")
    private Boolean previouslyMarried;

    @Column(name = "profession_type", length = 80)
    private String professionType;

    @Column(name = "profession_title", length = 150)
    private String professionTitle;

    @Column(name = "monthly_income")
    private Integer monthlyIncome;

    @Column(name = "mehr_offered")
    private Integer mehrOffered;

    @Column(name = "mehr_minimum_expected")
    private Integer mehrMinimumExpected;

    @Column(name = "house_type", length = 50)
    private String houseType;

    @Column(name = "family_type", length = 50)
    private String familyType;

    @Column(name = "expectations_text", length = 500)
    private String expectationsText;

    @Column(name = "profile_status", nullable = false, length = 40)
    private String profileStatus;

    @Column(name = "completion_pct", nullable = false)
    private Short completionPct;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (profileStatus == null) profileStatus = "INCOMPLETE";
        if (completionPct == null) completionPct = (short) 0;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}