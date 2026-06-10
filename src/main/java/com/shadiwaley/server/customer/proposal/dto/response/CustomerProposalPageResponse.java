package com.shadiwaley.server.customer.proposal.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerProposalPageResponse {

    private List<CustomerProposalItemResponse> items;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}