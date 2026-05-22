package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AutopilotSuggestionItemResponse {

    private UUID profileId;
    private UUID userId;

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

    private boolean verified;
    private boolean hasProfilePhoto;
    private String profilePhotoViewUrl;

    private Integer matchScore;
    private Integer compatibilityScore;

    private String dispatchReadiness;

    private boolean alreadyDispatched;
    private boolean alreadyProposed;

    private boolean canDispatch;
    private String blockReason;

    private List<String> reasons;
    private List<String> riskFlags;
}