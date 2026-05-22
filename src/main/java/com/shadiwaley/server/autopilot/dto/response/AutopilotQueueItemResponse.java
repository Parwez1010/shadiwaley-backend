package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotQueueStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AutopilotQueueItemResponse {

    private UUID queueId;
    private UUID userId;
    private UUID profileId;
    private UUID crmCaseId;

    private String candidateName;
    private String parentName;
    private String parentPhone;
    private String side;
    private String district;
    private String state;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    private String planCode;
    private String planName;
    private String paymentStatus;
    private String subscriptionStatus;

    private Integer profileCompletionPct;
    private boolean verified;
    private boolean hasProfilePhoto;
    private String photoSharingConsent;

    private AutopilotQueueStatus queueStatus;
    private Integer priorityScore;
    private String reason;

    private Instant lastDispatchAt;
    private Instant nextDispatchDueAt;
    private Integer dispatchCount;
    private Integer pendingResponseCount;

    private String blockedReason;
    private boolean overdue;

    private Instant createdAt;
    private Instant updatedAt;
}