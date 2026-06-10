package com.shadiwaley.server.customer.proposal.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerProposalDetailResponse {

    private CustomerProposalItemResponse proposal;

    private String fullProfileAvailableMessage;
}