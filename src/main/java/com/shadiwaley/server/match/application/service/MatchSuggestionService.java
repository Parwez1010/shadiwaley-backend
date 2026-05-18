package com.shadiwaley.server.match.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.match.dto.response.CompatibilityResponse;
import com.shadiwaley.server.match.dto.response.MatchFilterMetadataResponse;
import com.shadiwaley.server.match.dto.response.MatchSourceProfileResponse;
import com.shadiwaley.server.match.dto.response.MatchSuggestionPageResponse;
import com.shadiwaley.server.match.dto.response.MatchSuggestionResponse;
import com.shadiwaley.server.match.dto.response.ProposalEligibilityResult;
import com.shadiwaley.server.match.dto.response.ScoreRangeResponse;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchSuggestionService {

    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final CrmCaseRepository crmCaseRepository;
    private final ProposalRepository proposalRepository;

    private final MatchScoreEngineService matchScoreEngineService;
    private final ProposalEligibilityService proposalEligibilityService;
    private final AuditLogService auditLogService;
    private final MediaFileRepository mediaFileRepository;

    @Transactional(readOnly = true)
    public MatchSuggestionPageResponse getSuggestions(
            UUID profileId,
            int page,
            int size,
            String search,
            String district,
            String state,
            String caste,
            String maslak,
            Short minAge,
            Short maxAge,
            String education,
            String professionType,
            String familyType,
            Integer minIncome,
            Integer maxIncome,
            Boolean verifiedOnly,
            Boolean hasPhotoOnly,
            Boolean notPreviouslyProposed,
            String readiness,
            Integer minScore,
            Integer maxScore,
            String sort
    ) {
        UserProfile source = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Source profile not found"));

        auditLogService.record(
                AuditAction.MATCH_SUGGESTIONS_VIEWED,
                AuditEntityType.USER_PROFILE,
                source.getId(),
                "Match suggestions viewed"
        );


        boolean filtersApplied =
                search != null
                        || district != null
                        || state != null
                        || caste != null
                        || maslak != null
                        || minAge != null
                        || maxAge != null
                        || education != null
                        || professionType != null
                        || familyType != null
                        || minIncome != null
                        || maxIncome != null
                        || verifiedOnly != null
                        || hasPhotoOnly != null
                        || notPreviouslyProposed != null
                        || readiness != null
                        || minScore != null
                        || maxScore != null;

        if (filtersApplied) {
            auditLogService.record(
                    AuditAction.MATCH_FILTER_APPLIED,
                    AuditEntityType.USER_PROFILE,
                    source.getId(),
                    "Match filters applied"
            );
        }


        ParentProfile sourceParent = parentProfileRepository
                .findByUserAccountId(source.getUserAccount().getId())
                .orElse(null);

        UserPreferences preferences = userPreferencesRepository
                .findByUserProfileId(source.getId())
                .orElse(null);

        CrmCase crmCase = crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(source.getUserAccount().getId())
                .orElse(null);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );

        Page<UserProfile> candidatePage = userProfileRepository.findAllEligibleMatches(
                source.getUserAccount().getSide(),
                source.getId(),
                pageable
        );

        List<MatchSuggestionResponse> suggestions = candidatePage
                .getContent()
                .stream()
                .map(candidate -> toSuggestionResponse(
                        crmCase,
                        source,
                        candidate,
                        sourceParent,
                        preferences
                ))
                .sorted(
                        Comparator.comparing(
                                MatchSuggestionResponse::getCompatibilityScore,
                                Comparator.nullsLast(Integer::compareTo)
                        ).reversed()
                )
                .toList();

        List<MatchSuggestionResponse> filtered = suggestions.stream()
                .filter(m -> minScore == null || m.getCompatibilityScore() >= minScore)
                .filter(m -> maxScore == null || m.getCompatibilityScore() <= maxScore)
                .filter(m -> readiness == null || readiness.equalsIgnoreCase(m.getDispatchReadiness()))
                .filter(m -> !Boolean.TRUE.equals(hasPhotoOnly) || m.isHasProfilePhoto())
                .filter(m -> !Boolean.TRUE.equals(notPreviouslyProposed) || !m.isAlreadyProposed())
                .toList();


        return MatchSuggestionPageResponse.builder()
                .sourceProfile(toSourceProfileResponse(source, sourceParent, crmCase))
                .matches(suggestions != null ? suggestions : List.of())
                .filters(defaultFilters())
                .page(candidatePage.getNumber())
                .size(candidatePage.getSize())
                .totalElements(candidatePage.getTotalElements())
                .totalPages(candidatePage.getTotalPages())
                .last(candidatePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public CompatibilityResponse compatibility(
            UUID profileAId,
            UUID profileBId
    ) {
        UserProfile source = userProfileRepository.findById(profileAId)
                .orElseThrow(() -> new EntityNotFoundException("Source profile not found"));

        UserProfile target = userProfileRepository.findById(profileBId)
                .orElseThrow(() -> new EntityNotFoundException("Target profile not found"));

        auditLogService.record(
                AuditAction.COMPATIBILITY_VIEWED,
                AuditEntityType.USER_PROFILE,
                source.getId(),
                "Compatibility viewed with profile " + target.getId()
        );


        ParentProfile sourceParent = parentProfileRepository
                .findByUserAccountId(source.getUserAccount().getId())
                .orElse(null);

        ParentProfile targetParent = parentProfileRepository
                .findByUserAccountId(target.getUserAccount().getId())
                .orElse(null);

        UserPreferences preferences = userPreferencesRepository
                .findByUserProfileId(source.getId())
                .orElse(null);

        return matchScoreEngineService.calculate(
                source,
                target,
                sourceParent,
                targetParent,
                preferences
        );
    }

    private MatchSuggestionResponse toSuggestionResponse(
            CrmCase crmCase,
            UserProfile source,
            UserProfile candidate,
            ParentProfile sourceParent,
            UserPreferences preferences
    ) {
        ParentProfile targetParent = parentProfileRepository
                .findByUserAccountId(candidate.getUserAccount().getId())
                .orElse(null);

        CompatibilityResponse compatibility = matchScoreEngineService.calculate(
                source,
                candidate,
                sourceParent,
                targetParent,
                preferences
        );

        Proposal latestProposal = proposalRepository
                .findTopByProfiles(source.getId(), candidate.getId())
                .orElse(null);

        boolean alreadyProposed = latestProposal != null;

        boolean alreadyAccepted = latestProposal != null
                && latestProposal.getStatus() == ProposalStatus.ACCEPTED;

        ProposalEligibilityResult eligibility =
                proposalEligibilityService.checkEligibility(
                        crmCase,
                        source,
                        candidate,
                        alreadyProposed,
                        alreadyAccepted,
                        compatibility.getTotalScore()
                );

        MediaFile profilePhoto = mediaFileRepository
                .findTopByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalseOrderByCreatedAtDesc(
                        candidate.getId(),
                        MediaType.PROFILE_PHOTO
                )
                .orElse(null);

        boolean hasProfilePhoto = profilePhoto != null;

        boolean profilePhotoVerified = profilePhoto != null
                && profilePhoto.getReviewStatus() == MediaReviewStatus.APPROVED;

        return MatchSuggestionResponse.builder()
                .profileId(candidate.getId())
                .userId(candidate.getUserAccount().getId())
                .displayId(candidate.getDisplayId())
                .candidateName(candidate.getCandidateFirstName())

                .parentName(targetParent != null ? targetParent.getParentName() : null)
                .parentPhone(targetParent != null ? targetParent.getParentPhone() : null)

                .side(candidate.getUserAccount().getSide() != null
                        ? candidate.getUserAccount().getSide().name()
                        : null)

                .age(candidate.getCandidateAge() != null
                        ? Integer.valueOf(candidate.getCandidateAge())
                        : null)

                .heightCm(candidate.getCandidateHeightCm() != null
                        ? candidate.getCandidateHeightCm().intValue()
                        : null)

                .district(targetParent != null ? targetParent.getDistrict() : null)
                .state(targetParent != null ? targetParent.getState() : null)
                .caste(targetParent != null ? targetParent.getCaste() : null)
                .maslak(targetParent != null ? targetParent.getMaslak() : null)

                .education(candidate.getEducation())
                .professionType(candidate.getProfessionType())
                .professionTitle(candidate.getProfessionTitle())
                .monthlyIncome(candidate.getMonthlyIncome())
                .familyType(candidate.getFamilyType())

                .profileStatus(candidate.getProfileStatus() != null
                        ? candidate.getProfileStatus().name()
                        : null)

                .completionPct(
                        candidate.getCompletionPct() != null
                                ? candidate.getCompletionPct().intValue()
                                : null
                )
                .verified(candidate.getProfileStatus() != null
                        && (
                        candidate.getProfileStatus().name().equals("LIVE")
                                || candidate.getProfileStatus().name().equals("VERIFIED")
                ))

                .hasProfilePhoto(hasProfilePhoto)
                .profilePhotoMediaId(profilePhoto != null ? profilePhoto.getId() : null)
                .profilePhotoViewUrl(profilePhoto != null
                        ? "/api/v1/admin/review/media/" + profilePhoto.getId() + "/view"
                        : null)
                .profilePhotoVerified(profilePhotoVerified)
                .idProofVerified(false)
                .incomeProofVerified(false)

                .compatibilityScore(compatibility.getTotalScore())
                .matchGrade(compatibility.getMatchGrade())

                .dispatchReadiness(eligibility.getDispatchReadiness())

                .alreadyProposed(alreadyProposed)
                .alreadyAccepted(alreadyAccepted)

                .latestProposalId(latestProposal != null ? latestProposal.getId() : null)
                .latestProposalStatus(latestProposal != null
                        ? latestProposal.getStatus().name()
                        : null)
                .lastProposedAt(latestProposal != null
                        ? latestProposal.getDispatchedAt()
                        : null)

                .canSendProposal(eligibility.isCanSendProposal())
                .proposalBlockReason(eligibility.getProposalBlockReason())

                .reasons(compatibility.getReasons())
                .riskFlags(compatibility.getRiskFlags())
                .compatibilityBreakdown(compatibility.getBreakdown())

                .build();
    }

    private MatchSourceProfileResponse toSourceProfileResponse(
            UserProfile source,
            ParentProfile parent,
            CrmCase crmCase
    ) {
        return MatchSourceProfileResponse.builder()
                .profileId(source.getId())
                .userId(source.getUserAccount().getId())
                .crmCaseId(crmCase != null ? crmCase.getId() : null)

                .displayId(source.getDisplayId())
                .candidateName(source.getCandidateFirstName())

                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)

                .side(source.getUserAccount().getSide() != null
                        ? source.getUserAccount().getSide().name()
                        : null)

                .age(source.getCandidateAge() != null
                        ? Integer.valueOf(source.getCandidateAge())
                        : null)

                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .maslak(parent != null ? parent.getMaslak() : null)

                .education(source.getEducation())
                .profileStatus(source.getProfileStatus())

                .assignedEmployeeId(crmCase != null && crmCase.getAssignedEmployee() != null
                        ? crmCase.getAssignedEmployee().getId()
                        : null)

                .assignedEmployeeName(crmCase != null && crmCase.getAssignedEmployee() != null
                        ? crmCase.getAssignedEmployee().getFullName()
                        : null)

                .build();
    }

    private MatchFilterMetadataResponse defaultFilters() {
        return MatchFilterMetadataResponse.builder()
                .districts(parentProfileRepository.findDistinctDistricts())
                .states(parentProfileRepository.findDistinctStates())
                .castes(parentProfileRepository.findDistinctCastes())
                .maslak(parentProfileRepository.findDistinctMaslak())
                .education(userProfileRepository.findDistinctEducation())
                .professionTypes(userProfileRepository.findDistinctProfessionTypes())
                .familyTypes(userProfileRepository.findDistinctFamilyTypes())
                .scoreRanges(List.of(
                        ScoreRangeResponse.builder().label("Excellent 80+").min(80).max(100).build(),
                        ScoreRangeResponse.builder().label("Strong 60-79").min(60).max(79).build(),
                        ScoreRangeResponse.builder().label("Moderate 40-59").min(40).max(59).build(),
                        ScoreRangeResponse.builder().label("Low below 40").min(0).max(39).build()
                ))
                .build();
    }
    @Transactional(readOnly = true)
    public MatchSuggestionResponse getCandidatePreview(
            UUID sourceProfileId,
            UUID targetProfileId
    ) {
        UserProfile source = userProfileRepository.findById(sourceProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Source profile not found"));

        UserProfile target = userProfileRepository.findById(targetProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Target profile not found"));

        auditLogService.record(
                AuditAction.CANDIDATE_PREVIEW_OPENED,
                AuditEntityType.USER_PROFILE,
                target.getId(),
                "Candidate preview opened from Find Matches"
        );

        ParentProfile sourceParent = parentProfileRepository
                .findByUserAccountId(source.getUserAccount().getId())
                .orElse(null);

        UserPreferences preferences = userPreferencesRepository
                .findByUserProfileId(source.getId())
                .orElse(null);

        CrmCase crmCase = crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(source.getUserAccount().getId())
                .orElse(null);

        return toSuggestionResponse(
                crmCase,
                source,
                target,
                sourceParent,
                preferences
        );
    }
}