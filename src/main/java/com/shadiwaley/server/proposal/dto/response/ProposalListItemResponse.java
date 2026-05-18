package com.shadiwaley.server.proposal.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ProposalListItemResponse {

    private UUID proposalId;

    private UUID fromProfileId;
    private UUID toProfileId;

    private String fromCandidateName;
    private String toCandidateName;

    private String fromSide;
    private String toSide;

    private String toParentName;
    private String toParentPhone;
    private String toDistrict;
    private String toCaste;
    private String toMaslak;

    private Integer matchScore;

    private ProposalStatus status;
    private ProposalDispatchChannel dispatchChannel;

    private String note;
    private String dispatchedByName;
    private Instant dispatchedAt;
    private Instant lastUpdatedAt;
}