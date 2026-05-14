package com.shadiwaley.server.verification.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReviewQueueProfileResponse {

    private UUID profileId;
    private UUID userId;

    private String candidateName;
    private String phone;

    private Short completionPct;

    private ProfileStatus profileStatus;

    private boolean hasProfilePhoto;
    private boolean hasIdProof;
    private boolean hasIncomeProof;

    private String familyName;
    private String parentName;
    private String parentRelation;

    private String side;

    private String district;
    private String state;

    private String maslak;
    private String caste;

    private Boolean profilePhotoVerified;
    private Boolean idProofVerified;
    private Boolean incomeProofVerified;



    private Instant createdAt;
}