package com.shadiwaley.server.proposal.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.crm.domain.CrmCaseStage;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.domain.ProposalSourceType;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.dto.request.SendProposalRequest;
import com.shadiwaley.server.proposal.dto.response.ProposalResponse;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.entity.ProposalStatusHistory;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalStatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProposalDispatchService {

    private final ProposalRepository proposalRepository;
    private final ProposalStatusHistoryRepository proposalStatusHistoryRepository;
    private final UserProfileRepository userProfileRepository;
    private final CrmCaseRepository crmCaseRepository;
    private final ProposalPermissionService permissionService;
    private final ProposalMatchScoreService matchScoreService;
    private final ProposalTimelineService proposalTimelineService;
    private final AuditLogService auditLogService;

    @Value("${proposal.resend-cooldown-days:30}")
    private int resendCooldownDays;

    private static final List<ProposalStatus> ACTIVE_STATUSES = List.of(
            ProposalStatus.SENT,
            ProposalStatus.VIEWED,
            ProposalStatus.INTERESTED,
            ProposalStatus.SHORTLISTED,
            ProposalStatus.MEETING_DISCUSSION,
            ProposalStatus.ACCEPTED
    );

    @Transactional
    public ProposalResponse sendProposal(SendProposalRequest request) {
        UserProfile fromProfile = userProfileRepository.findById(request.getFromProfileId())
                .orElseThrow(() -> new EntityNotFoundException("From profile not found"));

        UserProfile toProfile = userProfileRepository.findById(request.getToProfileId())
                .orElseThrow(() -> new EntityNotFoundException("To profile not found"));

        validateProfiles(fromProfile, toProfile);

        CrmCase crmCase = null;
        if (request.getCrmCaseId() != null) {
            crmCase = crmCaseRepository.findById(request.getCrmCaseId())
                    .orElseThrow(() -> new EntityNotFoundException("CRM case not found"));

            permissionService.assertCanDispatchForCase(crmCase);

            if (!crmCase.getUserProfile().getId().equals(fromProfile.getId())) {
                throw new IllegalArgumentException("CRM case does not belong to fromProfileId");
            }
        }

        assertNoActiveDuplicate(fromProfile.getId(), toProfile.getId());

        EmployeeAccount actor = permissionService.getCurrentEmployeeOrThrow();

        ProposalMatchScoreService.MatchScoreResult scoreResult =
                matchScoreService.calculate(fromProfile, toProfile);

        Proposal proposal = new Proposal();
        proposal.setFromProfile(fromProfile);
        proposal.setToProfile(toProfile);
        proposal.setCrmCase(crmCase);
        proposal.setStatus(ProposalStatus.SENT);
        proposal.setDispatchChannel(request.getDispatchChannel());
        proposal.setSourceType(
                request.getSourceType() != null
                        ? request.getSourceType()
                        : ProposalSourceType.MANUAL
        );
        proposal.setNote(request.getNote());
        proposal.setShareProfilePhoto(request.isShareProfilePhoto());
        proposal.setMatchScore(scoreResult.getScore());
        proposal.setDispatchedByEmployee(actor);
        proposal.setDispatchedByName(actor.getFullName());
        proposal.setDispatchedAt(Instant.now());
        proposal.setLastUpdatedByEmployee(actor);
        proposal.setLastUpdatedByName(actor.getFullName());
        proposal.setLastUpdatedAt(Instant.now());

        Proposal saved = proposalRepository.save(proposal);

        if (proposal.getSourceType() == ProposalSourceType.FIND_MATCHES) {

            auditLogService.record(
                    AuditAction.PROPOSAL_SENT_FROM_FIND_MATCHES,
                    AuditEntityType.PROPOSAL,
                    proposal.getId(),
                    "Proposal sent from Find Matches"
            );
        }

        if (proposal.getSourceType() == ProposalSourceType.ASSISTED_DISPATCH) {

            auditLogService.record(
                    AuditAction.PROPOSAL_SENT_FROM_ASSISTED_DISPATCH,
                    AuditEntityType.PROPOSAL,
                    proposal.getId(),
                    "Proposal sent from Assisted Dispatch"
            );
        }


        createHistory(saved, ProposalStatus.SENT, request.getNote(), actor);

        if (crmCase != null) {
            crmCase.setStage(CrmCaseStage.MATCH_SUGGESTED);
            crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            crmCase.setLastOutcome("Proposal sent");
            crmCaseRepository.save(crmCase);

            proposalTimelineService.addProposalTimeline(
                    crmCase,
                    CrmTimelineEventType.PROPOSAL_SENT,
                    "Proposal sent",
                    "Proposal sent from "
                            + fromProfile.getCandidateFirstName()
                            + " to "
                            + toProfile.getCandidateFirstName(),
                    saved
            );
        }

        return toResponse(saved);
    }

    private void validateProfiles(UserProfile fromProfile, UserProfile toProfile) {
        if (fromProfile.getId().equals(toProfile.getId())) {
            throw new IllegalArgumentException("Cannot send proposal to same profile");
        }

        if (fromProfile.getUserAccount().getSide() == null || toProfile.getUserAccount().getSide() == null) {
            throw new IllegalArgumentException("Both profiles must have side");
        }

        if (fromProfile.getUserAccount().getSide() == toProfile.getUserAccount().getSide()) {
            throw new IllegalArgumentException("Proposal can be sent only to opposite side profile");
        }

        if (!isEligible(fromProfile) || !isEligible(toProfile)) {
            throw new IllegalArgumentException("Both profiles must be eligible for proposal");
        }
    }

    private boolean isEligible(UserProfile profile) {
        return profile.getProfileStatus() == ProfileStatus.LIVE
                || profile.getProfileStatus() == ProfileStatus.VERIFIED;
    }

    private void assertNoActiveDuplicate(UUID fromProfileId, UUID toProfileId) {
        boolean directExists =
                proposalRepository.existsByFromProfileIdAndToProfileIdAndStatusIn(
                        fromProfileId,
                        toProfileId,
                        ACTIVE_STATUSES
                );

        boolean reverseExists =
                proposalRepository.existsByFromProfileIdAndToProfileIdAndStatusIn(
                        toProfileId,
                        fromProfileId,
                        ACTIVE_STATUSES
                );

        if (directExists || reverseExists) {
            throw new IllegalArgumentException("Proposal already exists between these profiles");
        }
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

    private ProposalResponse toResponse(Proposal proposal) {
        return ProposalResponse.builder()
                .proposalId(proposal.getId())
                .fromProfileId(proposal.getFromProfile().getId())
                .toProfileId(proposal.getToProfile().getId())
                .crmCaseId(proposal.getCrmCase() != null ? proposal.getCrmCase().getId() : null)
                .status(proposal.getStatus())
                .dispatchChannel(proposal.getDispatchChannel())
                .note(proposal.getNote())
                .matchScore(proposal.getMatchScore())
                .shareProfilePhoto(proposal.isShareProfilePhoto())
                .dispatchedByName(proposal.getDispatchedByName())
                .dispatchedAt(proposal.getDispatchedAt())
                .lastUpdatedByName(proposal.getLastUpdatedByName())
                .lastUpdatedAt(proposal.getLastUpdatedAt())
                .build();
    }
}