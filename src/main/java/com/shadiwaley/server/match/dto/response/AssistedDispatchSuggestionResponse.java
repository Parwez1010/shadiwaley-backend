package com.shadiwaley.server.match.dto.response;

import com.shadiwaley.server.match.domain.DispatchReadiness;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AssistedDispatchSuggestionResponse {

    private UUID crmCaseId;

    private UUID sourceProfileId;

    private UUID targetProfileId;

    private String candidateName;
    private String parentName;
    private String parentPhone;

    private String side;
    private Integer age;
    private String district;
    private String state;
    private String caste;
    private String maslak;
    private String education;
    private String familyType;
    private String professionType;

    private Integer matchScore;
    private String matchGrade;

    private DispatchReadiness dispatchReadiness;

    private boolean alreadyDispatched;
    private String latestProposalStatus;

    private boolean hasProfilePhoto;
    private boolean verified;

    private List<String> matchReasons;
    private List<String> riskFlags;
}