package com.shadiwaley.server.admin.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmFamilyDetailResponse {

    private UUID userId;
    private UUID profileId;
    private String displayId;

    private String phone;
    private UserSide side;
    private String accountStatus;

    // Parent / family
    private String parentName;
    private String parentPhone;
    private String parentRelation;
    private String district;
    private String state;
    private String maslak;
    private String caste;
    private String imamReference;
    private String masjidName;

    // Candidate profile
    private String candidateName;
    private Short age;
    private Short heightCm;
    private String education;
    private String quranLevel;
    private String namaazRegularity;
    private Boolean previouslyMarried;
    private String professionType;
    private String professionTitle;
    private Integer monthlyIncome;
    private Integer mehrOffered;
    private String houseType;
    private String familyType;
    private String expectationsText;

    // Preferences
    private String preferredMaslak;
    private String preferredCaste;
    private String preferredState;
    private String preferredDistrict;
    private Short minAge;
    private Short maxAge;
    private String preferredEducation;
    private String preferredFamilyType;
    private Boolean requireImamRef;
    private Boolean requireIdVerified;

    // Status
    private Short completionPct;
    private ProfileStatus profileStatus;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}