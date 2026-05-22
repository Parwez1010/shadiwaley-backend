package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotDispatchItemStatus;
import com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DispatchDetailItemResponse {

    private UUID dispatchItemId;

    private UUID candidateProfileId;

    private UUID proposalId;

    private String candidateName;

    private String parentName;

    private String parentPhone;

    private String district;

    private Integer matchScore;

    private Integer compatibilityScore;

    private String dispatchReadiness;

    private AutopilotDispatchItemStatus status;

    private AutopilotResponseStatus responseStatus;

    private boolean photoIncluded;

    private String providerName;

    private String providerMessageId;

    private String deliveryStatus;

    private Instant deliveredAt;

    private Instant readAt;

    private Instant repliedAt;

    private String responseNote;

    private Instant respondedAt;
}