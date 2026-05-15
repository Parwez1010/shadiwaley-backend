package com.shadiwaley.server.verification.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ReviewProfileDetailResponse {

    private UUID profileId;
    private UUID userId;

    private String candidateName;
    private String side;
    private String phone;

    private Short completionPct;
    private ProfileStatus profileStatus;

    private Instant createdAt;
    private Instant submittedAt;

    private String familyName;
    private String parentName;
    private String parentRelation;
    private String parentPhone;
    private String district;
    private String state;
    private String maslak;
    private String caste;

    private Short age;
    private Short heightCm;
    private String education;
    private String quranLevel;
    private String namaazRegularity;
    private String professionType;
    private String professionTitle;
    private Integer monthlyIncome;
    private String houseType;
    private String familyType;
    private String expectationsText;

    private String preferredMaslak;
    private String preferredCaste;
    private String preferredState;
    private String preferredDistrict;
    private Short minAge;
    private Short maxAge;
    private String preferredEducation;
    private String preferredFamilyType;

    private Boolean hasProfilePhoto;
    private Boolean hasIdProof;
    private Boolean hasIncomeProof;

    private Boolean profilePhotoVerified;
    private Boolean idProofVerified;
    private Boolean incomeProofVerified;

    private Boolean imamRefVerified;
    private Boolean waliConsentRecorded;

    private List<ReviewMediaResponse> media;
}