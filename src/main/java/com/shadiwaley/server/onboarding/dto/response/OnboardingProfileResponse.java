package com.shadiwaley.server.onboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OnboardingProfileResponse {
    private UUID profileId;
    private Short completionPct;
    private String profileStatus;
    private List<MissingFieldResponse> missingFields;
}