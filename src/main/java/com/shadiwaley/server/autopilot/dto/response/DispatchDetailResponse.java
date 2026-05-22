package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotBatchStatus;
import com.shadiwaley.server.autopilot.domain.AutopilotDispatchChannel;
import com.shadiwaley.server.autopilot.domain.AutopilotSendMode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DispatchDetailResponse {

    private UUID dispatchBatchId;

    private UUID queueId;

    private UUID sourceProfileId;

    private String sourceCandidateName;

    private String sourceParentName;

    private String sourcePhone;

    private AutopilotBatchStatus status;

    private AutopilotDispatchChannel channel;

    private AutopilotSendMode sendMode;

    private boolean shareProfilePhoto;

    private String note;

    private Integer itemsCount;

    private String sentByName;

    private Instant sentAt;

    private String providerName;

    private String providerMessageId;

    private String deliveryStatus;

    private Instant deliveredAt;

    private Instant readAt;

    private Instant repliedAt;

    private List<DispatchDetailItemResponse> items;

    private Instant createdAt;

    private Instant updatedAt;
}