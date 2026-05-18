package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class CompatibilityResponse {

    private UUID profileAId;
    private UUID profileBId;

    // Always 0-100
    private Integer totalScore;

    private String matchGrade;

    private String recommendation;

    private String summary;

    private List<MatchReasonResponse> reasons;

    private List<MatchRiskFlagResponse> riskFlags;

    private Map<String, CompatibilityItemResponse> breakdown;
}