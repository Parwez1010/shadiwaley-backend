package com.shadiwaley.server.rishtapipeline.application.service;

import com.shadiwaley.server.crm.infrastructure.entity.CrmFollowUp;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import com.shadiwaley.server.rishtapipeline.dto.response.RishtaPipelineItemResponse;
import com.shadiwaley.server.rishtapipeline.infrastructure.entity.RishtaPipelineNote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RishtaPipelineMapper {

    private final RishtaPipelineStageResolver stageResolver;

    public RishtaPipelineItemResponse toResponse(
            Proposal proposal,
            ParentProfile fromParent,
            ParentProfile toParent,
            CrmFollowUp latestFollowUp,
            RishtaPipelineNote latestNote,
            SubscriptionResponse fromSubscription
    ) {

        RishtaPipelineStage stage =
                stageResolver.resolve(proposal.getStatus());

        Instant lastActivityAt = resolveLastActivityAt(
                proposal,
                latestFollowUp,
                latestNote
        );

        boolean overdue =
                latestFollowUp != null
                        && latestFollowUp.getScheduledAt() != null
                        && latestFollowUp.getScheduledAt().isBefore(Instant.now())
                        && (
                        latestFollowUp.getStatus() == null
                                || !"COMPLETED".equalsIgnoreCase(latestFollowUp.getStatus().name())
                );

        long ageInDays = Duration.between(
                proposal.getCreatedAt(),
                Instant.now()
        ).toDays();

        return RishtaPipelineItemResponse.builder()
                .pipelineId(proposal.getId())
                .proposalId(proposal.getId())

                .crmCaseId(
                        proposal.getCrmCase() != null
                                ? proposal.getCrmCase().getId()
                                : null
                )

                .fromProfileId(
                        proposal.getFromProfile() != null
                                ? proposal.getFromProfile().getId()
                                : null
                )

                .toProfileId(
                        proposal.getToProfile() != null
                                ? proposal.getToProfile().getId()
                                : null
                )

                .fromCandidateName(
                        proposal.getFromProfile() != null
                                ? proposal.getFromProfile().getCandidateFirstName()
                                : null
                )

                .fromParentName(
                        fromParent != null
                                ? fromParent.getParentName()
                                : null
                )

                .fromPhone(
                        proposal.getFromProfile() != null
                                && proposal.getFromProfile().getUserAccount() != null
                                ? proposal.getFromProfile().getUserAccount().getPhone()
                                : null
                )

                .fromSide(
                        proposal.getFromProfile() != null
                                && proposal.getFromProfile().getUserAccount() != null
                                ? proposal.getFromProfile().getUserAccount().getSide().name()
                                : null
                )
                .fromDistrict(
                        fromParent != null
                                ? fromParent.getDistrict()
                                : null
                )

                .toCandidateName(
                        proposal.getToProfile() != null
                                ? proposal.getToProfile().getCandidateFirstName()
                                : null
                )

                .toParentName(
                        toParent != null
                                ? toParent.getParentName()
                                : null
                )

                .toPhone(
                        proposal.getToProfile() != null
                                && proposal.getToProfile().getUserAccount() != null
                                ? proposal.getToProfile().getUserAccount().getPhone()
                                : null
                )

                .toSide(
                        proposal.getToProfile() != null
                                && proposal.getToProfile().getUserAccount() != null
                                ? proposal.getToProfile().getUserAccount().getSide().name()
                                : null
                )

                .toDistrict(
                        toParent != null
                                ? toParent.getDistrict()
                                : null
                )

                .matchScore(proposal.getMatchScore())

                .status(proposal.getStatus())

                .pipelineStage(stage)
                .pipelineStageLabel(stageResolver.label(stage))

                .dispatchChannel(proposal.getDispatchChannel())
                .sourceType(proposal.getSourceType())

                .assignedEmployeeId(
                        proposal.getCrmCase() != null
                                && proposal.getCrmCase().getAssignedEmployee() != null
                                ? proposal.getCrmCase().getAssignedEmployee().getId()
                                : null
                )

                .assignedEmployeeName(
                        proposal.getCrmCase() != null
                                && proposal.getCrmCase().getAssignedEmployee() != null
                                ? proposal.getCrmCase().getAssignedEmployee().getFullName()
                                : null
                )

                .sentAt(proposal.getDispatchedAt())
                .viewedAt(null)
                .lastStatusAt(proposal.getUpdatedAt())
                .lastActivityAt(lastActivityAt)

                .nextFollowUpAt(
                        latestFollowUp != null
                                ? latestFollowUp.getScheduledAt()
                                : null
                )

                .overdue(overdue)

                .ageInDays(ageInDays)

                .fromPlanName(
                        fromSubscription != null
                                ? fromSubscription.getPlanName()
                                : null
                )

                .fromPaymentStatus(
                        fromSubscription != null
                                && fromSubscription.getPaymentStatus() != null
                                ? fromSubscription.getPaymentStatus().name()
                                : null
                )

                .fromSubscriptionStatus(
                        fromSubscription != null
                                && fromSubscription.getSubscriptionStatus() != null
                                ? fromSubscription.getSubscriptionStatus().name()
                                : null
                )

                .lastNote(
                        latestNote != null
                                ? latestNote.getNote()
                                : null
                )

                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())

                .build();
    }

    private Instant resolveLastActivityAt(
            Proposal proposal,
            CrmFollowUp latestFollowUp,
            RishtaPipelineNote latestNote
    ) {
        Instant proposalUpdated =
                proposal.getUpdatedAt();

        Instant followUp =
                latestFollowUp != null
                        ? latestFollowUp.getUpdatedAt()
                        : null;

        Instant note =
                latestNote != null
                        ? latestNote.getCreatedAt()
                        : null;

        Instant latest = proposalUpdated;

        if (followUp != null && followUp.isAfter(latest)) {
            latest = followUp;
        }

        if (note != null && note.isAfter(latest)) {
            latest = note;
        }

        return latest;
    }
}