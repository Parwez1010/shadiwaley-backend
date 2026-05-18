package com.shadiwaley.server.match.application.service;

import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.match.domain.DispatchReadiness;
import com.shadiwaley.server.match.dto.response.AssistedDispatchResponse;
import com.shadiwaley.server.match.dto.response.AssistedDispatchSuggestionResponse;
import com.shadiwaley.server.match.dto.response.CompatibilityResponse;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.proposal.application.service.ProposalPermissionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssistedDispatchService {

    private final CrmCaseRepository crmCaseRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ProposalRepository proposalRepository;
    private final MatchScoreEngineService matchScoreEngineService;
    private final ProposalPermissionService proposalPermissionService;

    private static final List<ProposalStatus> ACTIVE_PROPOSAL_STATUSES = List.of(
            ProposalStatus.SENT,
            ProposalStatus.VIEWED,
            ProposalStatus.INTERESTED,
            ProposalStatus.SHORTLISTED,
            ProposalStatus.MEETING_DISCUSSION,
            ProposalStatus.ACCEPTED
    );

    @Transactional(readOnly = true)
    public AssistedDispatchResponse getAssistedDispatch(UUID crmCaseId) {

        CrmCase crmCase = crmCaseRepository.findById(crmCaseId)
                .orElseThrow(() -> new EntityNotFoundException("CRM case not found"));

        proposalPermissionService.assertCanAccessCase(crmCase);

        UserProfile source = crmCase.getUserProfile();

        ParentProfile sourceParent = parentProfileRepository
                .findByUserAccountId(source.getUserAccount().getId())
                .orElse(null);

        UserPreferences preferences = userPreferencesRepository
                .findByUserProfileId(source.getId())
                .orElse(null);

        List<UserProfile> candidates = userProfileRepository
                .findAllEligibleMatches(
                        source.getUserAccount().getSide(),
                        source.getId(),
                        PageRequest.of(0, 50)
                )
                .getContent();

        List<AssistedDispatchSuggestionResponse> suggestions = candidates
                .stream()
                .map(candidate -> buildSuggestion(crmCase, source, candidate, sourceParent, preferences))
                .sorted(
                        Comparator.comparing(
                                AssistedDispatchSuggestionResponse::getMatchScore,
                                Comparator.nullsLast(Integer::compareTo)
                        ).reversed()
                )
                .limit(10)
                .toList();

        return AssistedDispatchResponse.builder()
                .crmCaseId(crmCase.getId())
                .sourceProfileId(source.getId())
                .sourceCandidateName(source.getCandidateFirstName())
                .suggestions(suggestions)
                .build();
    }

    private AssistedDispatchSuggestionResponse buildSuggestion(
            CrmCase crmCase,
            UserProfile source,
            UserProfile target,
            ParentProfile sourceParent,
            UserPreferences preferences
    ) {
        ParentProfile targetParent = parentProfileRepository
                .findByUserAccountId(target.getUserAccount().getId())
                .orElse(null);

        CompatibilityResponse compatibility = matchScoreEngineService.calculate(
                source,
                target,
                sourceParent,
                targetParent,
                preferences
        );

        Proposal latestProposal = findLatestProposal(source.getId(), target.getId());

        boolean alreadyDispatched = latestProposal != null;

        DispatchReadiness readiness = resolveReadiness(
                compatibility.getTotalScore(),
                alreadyDispatched
        );

        List<String> reasons = compatibility.getReasons()
                .stream()
                .map(reason -> reason.getMessage())
                .toList();

        List<String> riskFlags = buildRiskFlags(target);

        return AssistedDispatchSuggestionResponse.builder()
                .crmCaseId(crmCase.getId())
                .sourceProfileId(source.getId())
                .targetProfileId(target.getId())
                .candidateName(target.getCandidateFirstName())
                .parentName(targetParent != null ? targetParent.getParentName() : null)
                .parentPhone(targetParent != null ? targetParent.getParentPhone() : null)
                .side(target.getUserAccount().getSide() != null ? target.getUserAccount().getSide().name() : null)
                .age(target.getCandidateAge() != null ? Integer.valueOf(target.getCandidateAge()) : null)
                .district(targetParent != null ? targetParent.getDistrict() : null)
                .state(targetParent != null ? targetParent.getState() : null)
                .caste(targetParent != null ? targetParent.getCaste() : null)
                .maslak(targetParent != null ? targetParent.getMaslak() : null)
                .education(target.getEducation())
                .familyType(target.getFamilyType())
                .professionType(target.getProfessionType())
                .matchScore(compatibility.getTotalScore())
                .matchGrade(resolveGrade(compatibility.getTotalScore()))
                .dispatchReadiness(readiness)
                .alreadyDispatched(alreadyDispatched)
                .latestProposalStatus(latestProposal != null ? latestProposal.getStatus().name() : null)
                .hasProfilePhoto(false)
                .verified(target.getProfileStatus() == ProfileStatus.LIVE || target.getProfileStatus() == ProfileStatus.VERIFIED)
                .matchReasons(reasons)
                .riskFlags(riskFlags)
                .build();
    }

    private Proposal findLatestProposal(UUID sourceProfileId, UUID targetProfileId) {
        return proposalRepository
                .findTopByFromProfileIdAndToProfileIdOrderByCreatedAtDesc(sourceProfileId, targetProfileId)
                .or(() -> proposalRepository.findTopByFromProfileIdAndToProfileIdOrderByCreatedAtDesc(targetProfileId, sourceProfileId))
                .orElse(null);
    }

    private DispatchReadiness resolveReadiness(Integer score, boolean alreadyDispatched) {
        if (alreadyDispatched) {
            return DispatchReadiness.ALREADY_DISPATCHED;
        }

        if (score != null && score >= 70) {
            return DispatchReadiness.READY_TO_SEND;
        }

        if (score != null && score >= 50) {
            return DispatchReadiness.NEEDS_REVIEW;
        }

        return DispatchReadiness.LOW_CONFIDENCE;
    }

    private String resolveGrade(Integer score) {
        if (score == null) return "LOW";
        if (score >= 85) return "EXCELLENT";
        if (score >= 70) return "STRONG";
        if (score >= 55) return "MODERATE";
        return "LOW";
    }

    private List<String> buildRiskFlags(UserProfile target) {
        List<String> flags = new ArrayList<>();

        if (target.getCompletionPct() == null || target.getCompletionPct() < 80) {
            flags.add("Profile completion is below 80%");
        }

        if (target.getProfileStatus() != ProfileStatus.LIVE && target.getProfileStatus() != ProfileStatus.VERIFIED) {
            flags.add("Profile is not fully live/verified");
        }

        return flags;
    }
}