package com.shadiwaley.server.proposal.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ProposalResponse {

    private UUID proposalId;

    private UUID fromProfileId;
    private UUID toProfileId;
    private UUID crmCaseId;

    private ProposalStatus status;
    private ProposalDispatchChannel dispatchChannel;

    private String note;
    private Integer matchScore;

    private boolean shareProfilePhoto;

    private String dispatchedByName;
    private Instant dispatchedAt;

    private String lastUpdatedByName;
    private Instant lastUpdatedAt;
}