package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AutopilotSuggestionSourceProfileResponse {

    private UUID userId;
    private UUID profileId;

    private String candidateName;
    private String side;
    private String district;

    private String planName;
}