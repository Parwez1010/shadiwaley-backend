package com.shadiwaley.server.rishtapipeline.dto.request;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UpdateRishtaPipelineStageRequest {

    @NotNull
    private ProposalStatus status;

    private String note;

    private Instant nextFollowUpAt;
}