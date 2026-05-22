package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotBatchStatus;
import com.shadiwaley.server.autopilot.domain.AutopilotDispatchChannel;
import com.shadiwaley.server.autopilot.domain.AutopilotSendMode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DispatchHistoryItemResponse {

    private UUID dispatchBatchId;

    private UUID queueId;

    private UUID sourceProfileId;

    private String sourceCandidateName;

    private String sourceParentName;

    private String sourcePhone;

    private AutopilotBatchStatus status;

    private AutopilotDispatchChannel channel;

    private AutopilotSendMode sendMode;

    private Integer itemsCount;

    private String sentByName;

    private Instant sentAt;

    private String providerName;

    private String deliveryStatus;

    private Integer interestedResponses;

    private Integer acceptedResponses;

    private Integer rejectedResponses;

    private Integer noResponses;

    private Instant createdAt;
}