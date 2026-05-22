package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotQueueStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class QueueActionResponse {

    private UUID queueId;

    private AutopilotQueueStatus queueStatus;

    private Instant nextDispatchDueAt;

    private String reason;

    private Instant updatedAt;
}