package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotBatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SendDispatchResponse {

    private UUID dispatchBatchId;

    private UUID queueId;

    private AutopilotBatchStatus status;

    private Instant sentAt;

    private Integer itemsCount;

    private List<UUID> proposalIds;

    private boolean pipelineCreated;
}