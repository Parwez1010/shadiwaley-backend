package com.shadiwaley.server.profile.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProfileCompletionResponse {
    private UUID profileId;
    private Short completionPct;
    private String profileStatus;
    private boolean readyForReview;
    private List<MissingFieldResponse> missingFields;
}