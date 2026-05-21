package com.shadiwaley.server.rishtapipeline.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RishtaPipelineStageUpdateResponse {

    private UUID proposalId;

    private ProposalStatus status;
    private RishtaPipelineStage pipelineStage;
    private String pipelineStageLabel;

    private Instant lastStatusAt;
    private Instant nextFollowUpAt;
}