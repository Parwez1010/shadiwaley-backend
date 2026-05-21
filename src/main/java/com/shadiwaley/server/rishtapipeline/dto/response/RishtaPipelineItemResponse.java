package com.shadiwaley.server.rishtapipeline.dto.response;

import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalSourceType;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RishtaPipelineItemResponse {

    private UUID pipelineId;
    private UUID proposalId;
    private UUID crmCaseId;

    private UUID fromProfileId;
    private UUID toProfileId;

    private String fromCandidateName;
    private String fromParentName;
    private String fromPhone;
    private String fromSide;
    private String fromDistrict;

    private String toCandidateName;
    private String toParentName;
    private String toPhone;
    private String toSide;
    private String toDistrict;

    private Integer matchScore;

    private ProposalStatus status;
    private RishtaPipelineStage pipelineStage;
    private String pipelineStageLabel;

    private ProposalDispatchChannel dispatchChannel;
    private ProposalSourceType sourceType;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    private Instant sentAt;
    private Instant viewedAt;
    private Instant lastStatusAt;
    private Instant lastActivityAt;

    private Instant nextFollowUpAt;
    private boolean overdue;
    private Long ageInDays;

    private String fromPlanName;
    private String fromPaymentStatus;
    private String fromSubscriptionStatus;

    private String lastNote;

    private Instant createdAt;
    private Instant updatedAt;
}