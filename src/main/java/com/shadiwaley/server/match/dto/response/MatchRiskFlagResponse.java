package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchRiskFlagResponse {

    private String type;
    private String message;
    private String severity;
}