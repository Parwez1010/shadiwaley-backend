package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DispatchPreviewItemResponse {

    private UUID candidateProfileId;

    private String candidateName;

    private String summaryText;

    private Integer matchScore;

    private boolean photoIncluded;

    private String photoBlockedReason;
}