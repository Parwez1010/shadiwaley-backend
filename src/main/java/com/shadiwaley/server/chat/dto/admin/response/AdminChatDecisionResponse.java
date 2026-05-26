package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatFamilyDecision;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatDecisionResponse {

    private UUID roomId;
    private ChatFamilyDecision decision;
    private ProposalStatus proposalStatus;
    private RishtaPipelineStage pipelineStage;
    private Instant nextFollowUpAt;
}