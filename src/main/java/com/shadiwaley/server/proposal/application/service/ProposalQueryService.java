package com.shadiwaley.server.proposal.application.service;

import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.proposal.domain.ProposalDirection;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.dto.response.*;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.entity.ProposalStatusHistory;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalStatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProposalQueryService {

    private final ProposalRepository proposalRepository;
    private final ProposalStatusHistoryRepository proposalStatusHistoryRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ProposalPermissionService permissionService;

    @Transactional(readOnly = true)
    public ProposalPageResponse getProposals(
            UUID profileId,
            UUID crmCaseId,
            ProposalStatus status,
            ProposalDirection direction,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "dispatchedAt")
        );

        Page<Proposal> result = proposalRepository.findAll(
                specification(profileId, crmCaseId, status, direction),
                pageable
        );

        return ProposalPageResponse.builder()
                .proposals(result.getContent().stream().map(this::toListItem).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ProposalDetailResponse getProposalDetail(UUID proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        if (proposal.getCrmCase() != null) {
            permissionService.assertCanAccessCase(proposal.getCrmCase());
        }

        return ProposalDetailResponse.builder()
                .proposal(toProposalResponse(proposal))
                .fromProfile(toMiniProfile(proposal.getFromProfile()))
                .toProfile(toMiniProfile(proposal.getToProfile()))
                .statusHistory(
                        proposalStatusHistoryRepository
                                .findByProposalIdOrderByCreatedAtAsc(proposal.getId())
                                .stream()
                                .map(this::toHistoryResponse)
                                .toList()
                )
                .build();
    }

    private Specification<Proposal> specification(
            UUID profileId,
            UUID crmCaseId,
            ProposalStatus status,
            ProposalDirection direction
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (profileId != null) {
                if (direction == ProposalDirection.SENT_FROM_PROFILE) {
                    predicate = cb.and(predicate, cb.equal(root.get("fromProfile").get("id"), profileId));
                } else if (direction == ProposalDirection.RECEIVED_BY_PROFILE) {
                    predicate = cb.and(predicate, cb.equal(root.get("toProfile").get("id"), profileId));
                } else {
                    predicate = cb.and(
                            predicate,
                            cb.or(
                                    cb.equal(root.get("fromProfile").get("id"), profileId),
                                    cb.equal(root.get("toProfile").get("id"), profileId)
                            )
                    );
                }
            }

            if (crmCaseId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("crmCase").get("id"), crmCaseId));
            }

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            return predicate;
        };
    }

    private ProposalListItemResponse toListItem(Proposal proposal) {
        UserProfile from = proposal.getFromProfile();
        UserProfile to = proposal.getToProfile();

        ParentProfile toParent = parentProfileRepository
                .findByUserAccountId(to.getUserAccount().getId())
                .orElse(null);

        return ProposalListItemResponse.builder()
                .proposalId(proposal.getId())
                .fromProfileId(from.getId())
                .toProfileId(to.getId())
                .fromCandidateName(from.getCandidateFirstName())
                .toCandidateName(to.getCandidateFirstName())
                .fromSide(from.getUserAccount().getSide() != null ? from.getUserAccount().getSide().name() : null)
                .toSide(to.getUserAccount().getSide() != null ? to.getUserAccount().getSide().name() : null)
                .toParentName(toParent != null ? toParent.getParentName() : null)
                .toParentPhone(toParent != null ? toParent.getParentPhone() : null)
                .toDistrict(toParent != null ? toParent.getDistrict() : null)
                .toCaste(toParent != null ? toParent.getCaste() : null)
                .toMaslak(toParent != null ? toParent.getMaslak() : null)
                .matchScore(proposal.getMatchScore())
                .status(proposal.getStatus())
                .dispatchChannel(proposal.getDispatchChannel())
                .note(proposal.getNote())
                .dispatchedByName(proposal.getDispatchedByName())
                .dispatchedAt(proposal.getDispatchedAt())
                .lastUpdatedAt(proposal.getLastUpdatedAt())
                .build();
    }

    private ProposalResponse toProposalResponse(Proposal proposal) {
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

    private ProposalMiniProfileResponse toMiniProfile(UserProfile profile) {
        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(profile.getUserAccount().getId())
                .orElse(null);

        return ProposalMiniProfileResponse.builder()
                .profileId(profile.getId())
                .candidateName(profile.getCandidateFirstName())
                .side(profile.getUserAccount().getSide() != null ? profile.getUserAccount().getSide().name() : null)
                .age(
                        profile.getCandidateAge() != null
                                ? Integer.valueOf(profile.getCandidateAge())
                                : null
                )                .district(parent != null ? parent.getDistrict() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .build();
    }

    private ProposalStatusHistoryResponse toHistoryResponse(ProposalStatusHistory history) {
        return ProposalStatusHistoryResponse.builder()
                .status(history.getStatus())
                .note(history.getNote())
                .actorName(history.getActorName())
                .createdAt(history.getCreatedAt())
                .build();
    }
}