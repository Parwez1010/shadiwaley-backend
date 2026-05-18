package com.shadiwaley.server.proposal.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ProposalStatusUpdateResponse {

    private UUID proposalId;
    private ProposalStatus status;
    private String note;
    private String updatedByName;
    private Instant updatedAt;
}