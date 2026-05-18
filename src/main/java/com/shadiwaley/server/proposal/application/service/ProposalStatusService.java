package com.shadiwaley.server.proposal.application.service;

import com.shadiwaley.server.crm.domain.CrmCaseStage;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.dto.request.UpdateProposalStatusRequest;
import com.shadiwaley.server.proposal.dto.response.ProposalStatusUpdateResponse;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.entity.ProposalStatusHistory;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalStatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProposalStatusService {

    private final ProposalRepository proposalRepository;
    private final ProposalStatusHistoryRepository proposalStatusHistoryRepository;
    private final ProposalPermissionService permissionService;
    private final ProposalTimelineService proposalTimelineService;
    private final CrmCaseRepository crmCaseRepository;

    @Transactional
    public ProposalStatusUpdateResponse updateStatus(
            UUID proposalId,
            UpdateProposalStatusRequest request
    ) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        CrmCase crmCase = proposal.getCrmCase();

        if (crmCase != null) {
            permissionService.assertCanAccessCase(crmCase);
        } else {
            EmployeeAccount employee = permissionService.getCurrentEmployeeOrThrow();
            if (employee.getRole().name().equals("CRM_AGENT")) {
                throw new AccessDeniedException("CRM agent can update only assigned case proposals");
            }
        }

        EmployeeAccount actor = permissionService.getCurrentEmployeeOrThrow();

        proposal.setStatus(request.getStatus());
        proposal.setLastUpdatedByEmployee(actor);
        proposal.setLastUpdatedByName(actor.getFullName());
        proposal.setLastUpdatedAt(Instant.now());

        Proposal saved = proposalRepository.save(proposal);

        createHistory(saved, request.getStatus(), request.getNote(), actor);

        if (crmCase != null) {
            syncCrmCaseWithProposalStatus(crmCase, request.getStatus(), request.getNote());

            proposalTimelineService.addProposalTimeline(
                    crmCase,
                    resolveTimelineEvent(request.getStatus()),
                    resolveTimelineTitle(request.getStatus()),
                    request.getNote() != null && !request.getNote().isBlank()
                            ? request.getNote()
                            : "Proposal status changed to " + request.getStatus(),
                    saved
            );
        }

        return ProposalStatusUpdateResponse.builder()
                .proposalId(saved.getId())
                .status(saved.getStatus())
                .note(request.getNote())
                .updatedByName(actor.getFullName())
                .updatedAt(saved.getLastUpdatedAt())
                .build();
    }

    private void createHistory(
            Proposal proposal,
            ProposalStatus status,
            String note,
            EmployeeAccount actor
    ) {
        ProposalStatusHistory history = new ProposalStatusHistory();
        history.setProposal(proposal);
        history.setStatus(status);
        history.setNote(note);
        history.setActorEmployee(actor);
        history.setActorName(actor != null ? actor.getFullName() : "System");

        proposalStatusHistoryRepository.save(history);
    }

    private void syncCrmCaseWithProposalStatus(
            CrmCase crmCase,
            ProposalStatus status,
            String note
    ) {
        crmCase.setLastOutcome(
                note != null && !note.isBlank()
                        ? note
                        : "Proposal status changed to " + status
        );

        switch (status) {
            case SENT -> {
                crmCase.setStage(CrmCaseStage.MATCH_SUGGESTED);
                crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            }
            case INTERESTED, SHORTLISTED -> {
                crmCase.setStage(CrmCaseStage.FAMILY_INTERESTED);
                crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            }
            case MEETING_DISCUSSION -> {
                crmCase.setStage(CrmCaseStage.PROFILE_DISCUSSION);
                crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            }
            case ACCEPTED -> {
                crmCase.setStage(CrmCaseStage.CLOSED_SUCCESS);
                crmCase.setStatus(CrmCaseStatus.CLOSED);
                if (crmCase.getClosedAt() == null) {
                    crmCase.setClosedAt(Instant.now());
                }
            }
            case NOT_INTERESTED, REJECTED -> {
                crmCase.setStage(CrmCaseStage.FOLLOW_UP);
                crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            }
            case CANCELLED, EXPIRED -> {
                crmCase.setStatus(CrmCaseStatus.OPEN);
            }
            default -> {
            }
        }

        crmCaseRepository.save(crmCase);
    }

    private CrmTimelineEventType resolveTimelineEvent(ProposalStatus status) {
        return switch (status) {
            case SENT -> CrmTimelineEventType.PROPOSAL_SENT;
            case VIEWED -> CrmTimelineEventType.PROPOSAL_VIEWED;
            case INTERESTED -> CrmTimelineEventType.PROPOSAL_INTERESTED;
            case SHORTLISTED -> CrmTimelineEventType.PROPOSAL_SHORTLISTED;
            case MEETING_DISCUSSION -> CrmTimelineEventType.PROPOSAL_MEETING_DISCUSSION;
            case ACCEPTED -> CrmTimelineEventType.PROPOSAL_ACCEPTED;
            case REJECTED -> CrmTimelineEventType.PROPOSAL_REJECTED;
            case NOT_INTERESTED -> CrmTimelineEventType.PROPOSAL_NOT_INTERESTED;
            case CANCELLED -> CrmTimelineEventType.PROPOSAL_CANCELLED;
            case EXPIRED -> CrmTimelineEventType.PROPOSAL_EXPIRED;
        };
    }

    private String resolveTimelineTitle(ProposalStatus status) {
        return switch (status) {
            case SENT -> "Proposal sent";
            case VIEWED -> "Proposal viewed";
            case INTERESTED -> "Family interested";
            case SHORTLISTED -> "Proposal shortlisted";
            case MEETING_DISCUSSION -> "Meeting discussion started";
            case ACCEPTED -> "Proposal accepted";
            case REJECTED -> "Proposal rejected";
            case NOT_INTERESTED -> "Family not interested";
            case CANCELLED -> "Proposal cancelled";
            case EXPIRED -> "Proposal expired";
        };
    }
}