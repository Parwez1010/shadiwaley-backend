package com.shadiwaley.server.rishtapipeline.dto.response;

import com.shadiwaley.server.proposal.dto.response.ProposalResponse;
import com.shadiwaley.server.proposal.dto.response.ProposalStatusHistoryResponse;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RishtaPipelineDetailResponse {

    private RishtaPipelineItemResponse pipeline;

    private ProposalResponse proposal;

    private SubscriptionResponse subscription;

    private List<ProposalStatusHistoryResponse> statusHistory;

    private List<RishtaPipelineNoteResponse> notes;

    private List<PipelineFollowUpResponse> followUps;

    private List<PipelineTimelineResponse> timeline;

}