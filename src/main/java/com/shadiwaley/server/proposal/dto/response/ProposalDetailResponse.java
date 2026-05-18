package com.shadiwaley.server.proposal.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProposalDetailResponse {

    private ProposalResponse proposal;

    private ProposalMiniProfileResponse fromProfile;

    private ProposalMiniProfileResponse toProfile;

    private List<ProposalStatusHistoryResponse> statusHistory;
}