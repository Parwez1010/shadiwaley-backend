package com.shadiwaley.server.profile.infrastructure.entity;

import com.shadiwaley.server.profile.domain.*;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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

    @Column(length = 50)
    private String religion;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 40)
    private MaritalStatus maritalStatus;

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

    // ── Basic Information ──────────────────────────────
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(length = 30)
    private String complexion;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", length = 30)
    private BodyType bodyType;

    @Column(name = "mother_tongue", length = 50)
    private String motherTongue;

    @ElementCollection
    @CollectionTable(name = "user_profile_language", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "language")
    private Set<String> languagesKnown = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "family_values", length = 30)
    private FamilyValues familyValues;

    @Column(length = 50)
    private String sect;

    // ── Lifestyle ───────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Diet diet;

    @Column(name = "is_smoker")
    private Boolean smoker;

    @Column(name = "is_drinker")
    private Boolean drinker;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_frequency", length = 30)
    private ExerciseFrequency exerciseFrequency;

    @Column(name = "wears_hijab")
    private Boolean wearsHijab;

    // ── Family Details ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "family_status", length = 30)
    private FamilyStatus familyStatus;

    @Column(name = "brothers_count")
    private Short brothersCount;

    @Column(name = "sisters_count")
    private Short sistersCount;

    // ── Interests ────────────────────────────────────────
    @ElementCollection
    @CollectionTable(name = "user_profile_interest", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "interest")
    private Set<String> interests = new HashSet<>();


    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 40)
    private com.shadiwaley.server.profile.domain.ProfileStatus profileStatus;

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
        if (profileStatus == null) profileStatus = com.shadiwaley.server.profile.domain.ProfileStatus.INCOMPLETE;
        if (completionPct == null) completionPct = (short) 0;
        if (religion == null) religion = "ISLAM";

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}