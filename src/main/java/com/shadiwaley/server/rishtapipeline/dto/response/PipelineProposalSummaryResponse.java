package com.shadiwaley.server.rishtapipeline.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalSourceType;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PipelineProposalSummaryResponse {

    private UUID proposalId;
    private ProposalStatus status;
    private ProposalDispatchChannel dispatchChannel;
    private ProposalSourceType sourceType;
    private String note;
    private Integer matchScore;
    private String dispatchedByName;
    private Instant dispatchedAt;
    private Instant lastUpdatedAt;
}