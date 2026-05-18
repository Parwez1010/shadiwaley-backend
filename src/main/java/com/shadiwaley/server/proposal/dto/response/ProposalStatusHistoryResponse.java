package com.shadiwaley.server.proposal.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ProposalStatusHistoryResponse {

    private ProposalStatus status;
    private String note;
    private String actorName;
    private Instant createdAt;
}