package com.shadiwaley.server.customer.proposal.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerProposalSummaryResponse {

    private long sentTotal;
    private long receivedTotal;

    private long sentPending;
    private long receivedPending;

    private long accepted;
    private long rejected;
    private long expired;

    private long activeChats;
}