package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProposalEligibilityResult {

    private boolean canSendProposal;
    private String dispatchReadiness;
    private String proposalBlockReason;
}