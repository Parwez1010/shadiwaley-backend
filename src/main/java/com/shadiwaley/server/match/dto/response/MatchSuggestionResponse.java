package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class MatchSuggestionResponse {

    private UUID profileId;
    private UUID userId;

    private String displayId;
    private String candidateName;

    private String parentName;
    private String parentPhone;

    private String side;

    private Integer age;
    private Integer heightCm;

    private String district;
    private String state;
    private String caste;
    private String maslak;

    private String education;
    private String professionType;
    private String professionTitle;
    private Integer monthlyIncome;
    private String familyType;

    private String profileStatus;
    private Integer completionPct;

    private boolean verified;

    private boolean hasProfilePhoto;
    private UUID profilePhotoMediaId;
    private String profilePhotoViewUrl;

    private boolean profilePhotoVerified;
    private boolean idProofVerified;
    private boolean incomeProofVerified;

    // Find Matches score: always 0-100
    private Integer compatibilityScore;

    private String matchGrade;

    private String dispatchReadiness;

    private boolean alreadyProposed;
    private boolean alreadyAccepted;

    private UUID latestProposalId;
    private String latestProposalStatus;
    private Instant lastProposedAt;

    private boolean canSendProposal;
    private String proposalBlockReason;

    private List<MatchReasonResponse> reasons;

    private List<MatchRiskFlagResponse> riskFlags;

    private Map<String, CompatibilityItemResponse> compatibilityBreakdown;
}