package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RecordDispatchResponseResponse {

    private UUID dispatchItemId;

    private AutopilotResponseStatus responseStatus;

    private UUID proposalId;

    private RishtaPipelineStage pipelineStage;

    private Instant nextFollowUpAt;
}