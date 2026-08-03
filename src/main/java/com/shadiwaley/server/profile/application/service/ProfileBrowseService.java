package com.shadiwaley.server.profile.application.service;

import com.shadiwaley.server.customer.savedprofile.infrastructure.repository.SavedProfileRepository;
import com.shadiwaley.server.matchmaking.application.service.MatchScoreService;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.dto.request.BrowseProfileFilterRequest;
import com.shadiwaley.server.profile.dto.response.BrowseProfilesResponse;
import com.shadiwaley.server.profile.dto.response.MatchBreakdownResponse;
import com.shadiwaley.server.profile.dto.response.ProfileCardResponse;
import com.shadiwaley.server.profile.dto.response.ProfileDetailResponse;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.application.service.SubscriptionService;
import com.shadiwaley.server.subscription.domain.SubscriptionFeature;
import com.shadiwaley.server.user.domain.UserSide;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileBrowseService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MatchScoreService matchScoreService;
    private final SubscriptionService subscriptionService;

    private final RishtaRequestRepository rishtaRequestRepository;
    private final SavedProfileRepository savedProfileRepository;

    @Transactional(readOnly = true)
    public BrowseProfilesResponse browse(BrowseProfileFilterRequest filter) {
        UUID viewerUserId = AuthUser.getCurrentUserId();

        UserAccount viewerAccount = getViewerAccount(viewerUserId);
        UserProfile viewerProfile = getViewerProfile(viewerUserId);
        ParentProfile viewerParent = getViewerParent(viewerUserId);
        UserPreferences viewerPreferences = getViewerPreferences(viewerProfile.getId());

        Map<UUID, String> proposalStatusMap =
                getProposalStatusMap(viewerUserId);

        Set<UUID> savedProfileIds =
                getSavedProfileIds(viewerUserId);


        int page = filter.getPage() == null ? 0 : Math.max(filter.getPage(), 0);
        int size = filter.getSize() == null ? 10 : Math.min(Math.max(filter.getSize(), 1), 50);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserProfile> result = userProfileRepository.findAll(
                browseSpecification(viewerUserId, oppositeSide(viewerAccount.getSide()), filter),
                pageable
        );

        return BrowseProfilesResponse.builder()
                .profiles(result.getContent()
                        .stream()
                        .map(candidate -> toCard(
                                viewerProfile,
                                viewerParent,
                                viewerPreferences,
                                candidate,
                                proposalStatusMap,
                                savedProfileIds
                        )).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileDetail(UUID profileId) {
        UUID viewerUserId = AuthUser.getCurrentUserId();

        subscriptionService.validateFeatureAccess(
                viewerUserId,
                SubscriptionFeature.PROFILE_VIEW
        );

        UserAccount viewerAccount = getViewerAccount(viewerUserId);
        UserProfile viewerProfile = getViewerProfile(viewerUserId);
        ParentProfile viewerParent = getViewerParent(viewerUserId);
        UserPreferences viewerPreferences = getViewerPreferences(viewerProfile.getId());

        UserProfile candidate = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        if (candidate.getProfileStatus() != ProfileStatus.LIVE) {
            throw new IllegalArgumentException("This profile is not available for browsing");
        }

        if (candidate.getUserAccount().getId().equals(viewerUserId)) {
            throw new IllegalArgumentException("You cannot view your own profile in browse");
        }

        if (candidate.getUserAccount().getSide() != oppositeSide(viewerAccount.getSide())) {
            throw new IllegalArgumentException("This profile is not available for your account type");
        }

        ParentProfile candidateParent = getViewerParent(candidate.getUserAccount().getId());

        MatchBreakdownResponse match = matchScoreService.calculate(
                viewerProfile,
                viewerParent,
                viewerPreferences,
                candidate,
                candidateParent
        );

        subscriptionService.incrementProfileViewUsage(viewerUserId);

        return ProfileDetailResponse.builder()
                .profileId(candidate.getId())
                .displayId(candidate.getDisplayId())
                .side(candidate.getUserAccount().getSide())
                .firstName(candidate.getCandidateFirstName())
                .candidateAge(candidate.getCandidateAge())
                .candidateHeightCm(candidate.getCandidateHeightCm())
                .district(candidateParent.getDistrict())
                .state(candidateParent.getState())
                .maslak(candidateParent.getMaslak())
                .religion(candidate.getReligion())
                .maritalStatus(candidate.getMaritalStatus())
                .education(candidate.getEducation())
                .quranLevel(candidate.getQuranLevel())
                .namaazRegularity(candidate.getNamaazRegularity())
                .previouslyMarried(candidate.getPreviouslyMarried())
                .professionType(candidate.getProfessionType())
                .professionTitle(candidate.getProfessionTitle())
                .mehrOffered(candidate.getMehrOffered())
                .mehrMinimumExpected(candidate.getMehrMinimumExpected())
                .houseType(candidate.getHouseType())
                .familyType(candidate.getFamilyType())
                .expectationsText(candidate.getExpectationsText())
                .hasApprovedPhoto(hasApprovedProfilePhoto(candidate.getId()))
                .match(match)
                .build();
    }

    private Specification<UserProfile> browseSpecification(
            UUID viewerUserId,
            UserSide requiredSide,
            BrowseProfileFilterRequest filter
    ) {
        return (root, query, cb) -> {
            Join<UserProfile, UserAccount> accountJoin = root.join("userAccount", JoinType.INNER);

            var predicate = cb.conjunction();

            predicate = cb.and(predicate, cb.equal(root.get("profileStatus"), ProfileStatus.LIVE));
            predicate = cb.and(predicate, cb.equal(accountJoin.get("side"), requiredSide));
            predicate = cb.and(predicate, cb.notEqual(accountJoin.get("id"), viewerUserId));

            if (filter.getMinAge() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("candidateAge"), filter.getMinAge()));
            }

            if (filter.getMaxAge() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("candidateAge"), filter.getMaxAge()));
            }

            if (filter.getEducation() != null && !filter.getEducation().isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("education")), filter.getEducation().toLowerCase()));
            }

            if (filter.getFamilyType() != null && !filter.getFamilyType().isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("familyType")), filter.getFamilyType().toLowerCase()));
            }

            if (filter.getProfessionType() != null && !filter.getProfessionType().isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("professionType")), filter.getProfessionType().toLowerCase()));
            }

            if (filter.getQuranLevel() != null && !filter.getQuranLevel().isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("quranLevel")), filter.getQuranLevel().toLowerCase()));
            }

            if (filter.getNamaazRegularity() != null && !filter.getNamaazRegularity().isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("namaazRegularity")), filter.getNamaazRegularity().toLowerCase()));
            }

            if (filter.getMaritalStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("maritalStatus"), filter.getMaritalStatus()));
            }

            if (filter.getMinIncome() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("monthlyIncome"), filter.getMinIncome()));
            }

            if (filter.getMaxIncome() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("monthlyIncome"), filter.getMaxIncome()));
            }

            if (filter.getMinMehr() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("mehrOffered"), filter.getMinMehr()));
            }

            if (filter.getMaxMehr() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("mehrOffered"), filter.getMaxMehr()));
            }

            if (hasParentFilter(filter)) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<ParentProfile> parentRoot = subquery.from(ParentProfile.class);

                var parentPredicate = cb.equal(
                        parentRoot.get("userAccount").get("id"),
                        accountJoin.get("id")
                );

                if (filter.getCountry() != null && !filter.getCountry().isBlank()) {
                    parentPredicate = cb.and(parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("country")), filter.getCountry().toLowerCase()));
                }

                if (filter.getState() != null && !filter.getState().isBlank()) {
                    parentPredicate = cb.and(parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("state")), filter.getState().toLowerCase()));
                }

                if (filter.getDistrict() != null && !filter.getDistrict().isBlank()) {
                    parentPredicate = cb.and(parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("district")), filter.getDistrict().toLowerCase()));
                }

                if (filter.getMaslak() != null && !filter.getMaslak().isBlank()) {
                    parentPredicate = cb.and(parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("maslak")), filter.getMaslak().toLowerCase()));
                }

                subquery.select(parentRoot.get("userAccount").get("id"))
                        .where(parentPredicate);

                predicate = cb.and(predicate, cb.exists(subquery));
            }

            return predicate;
        };
    }

    private boolean hasParentFilter(BrowseProfileFilterRequest filter) {
        return hasText(filter.getCountry())
                || hasText(filter.getState())
                || hasText(filter.getDistrict())
                || hasText(filter.getMaslak());
    }

    private ProfileCardResponse toCard(
            UserProfile viewerProfile,
            ParentProfile viewerParent,
            UserPreferences viewerPreferences,
            UserProfile candidate,
            Map<UUID, String> proposalStatusMap,
            Set<UUID> savedProfileIds
    ){
        ParentProfile candidateParent = getViewerParent(candidate.getUserAccount().getId());

        MatchBreakdownResponse match = matchScoreService.calculate(
                viewerProfile,
                viewerParent,
                viewerPreferences,
                candidate,
                candidateParent
        );

        return ProfileCardResponse.builder()
                .profileId(candidate.getId())
                .displayId(candidate.getDisplayId())
                .side(candidate.getUserAccount().getSide())
                .firstName(candidate.getCandidateFirstName())
                .candidateFirstName(candidate.getCandidateFirstName())
                .candidateAge(candidate.getCandidateAge())
                .candidateHeightCm(candidate.getCandidateHeightCm())
                .district(candidateParent.getDistrict())
                .state(candidateParent.getState())
                .maslak(candidateParent.getMaslak())
                .religion(candidate.getReligion())
                .maritalStatus(candidate.getMaritalStatus())
                .education(candidate.getEducation())
                .quranLevel(candidate.getQuranLevel())
                .namaazRegularity(candidate.getNamaazRegularity())
                .professionType(candidate.getProfessionType())
                .professionTitle(candidate.getProfessionTitle())
                .familyType(candidate.getFamilyType())
                .proposalStatus(proposalStatusMap.get(candidate.getId()))
                .houseType(candidate.getHouseType())
                .mehrOffered(candidate.getMehrOffered())
                .mehrMinimumExpected(candidate.getMehrMinimumExpected())
                .expectationsText(candidate.getExpectationsText())
                .saved(savedProfileIds.contains(candidate.getId()))
                .hasApprovedPhoto(hasApprovedProfilePhoto(candidate.getId()))
                .match(match)
                .build();
    }

    private boolean hasApprovedProfilePhoto(UUID profileId) {
        return mediaFileRepository
                .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(profileId, MediaType.PROFILE_PHOTO)
                .filter(media -> media.getReviewStatus() == MediaReviewStatus.APPROVED)
                .isPresent();
    }

    private UserAccount getViewerAccount(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));
    }

    private UserProfile getViewerProfile(UUID userId) {
        return userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));
    }

    private ParentProfile getViewerParent(UUID userId) {
        return parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));
    }

    private UserPreferences getViewerPreferences(UUID profileId) {
        return userPreferencesRepository.findByUserProfileId(profileId)
                .orElseThrow(() -> new EntityNotFoundException("User preferences not found"));
    }

    private UserSide oppositeSide(UserSide side) {
        return side == UserSide.BOY ? UserSide.GIRL : UserSide.BOY;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Transactional(readOnly = true)
    public List<ProfileCardResponse> getTopMatches(int limit) {

        BrowseProfileFilterRequest filter =
                new BrowseProfileFilterRequest();

        filter.setPage(0);
        filter.setSize(limit);

        BrowseProfilesResponse response = browse(filter);

        return response.getProfiles();
    }

    private Map<UUID, String> getProposalStatusMap(UUID currentUserId) {

        return rishtaRequestRepository
                .findBySenderUserId(currentUserId)
                .stream()
                .collect(Collectors.toMap(
                        request -> request.getReceiverProfile().getId(),
                        request -> request.getStatus().name(),
                        (a, b) -> a
                ));
    }

    private Set<UUID> getSavedProfileIds(UUID userId) {

        return savedProfileRepository
                .findByUserAccountId(userId)
                .stream()
                .map(saved -> saved.getSavedProfile().getId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public ProfileCardResponse getProfileCard(UUID profileId) {
        UUID viewerUserId = AuthUser.getCurrentUserId();

        UserProfile viewerProfile = getViewerProfile(viewerUserId);
        ParentProfile viewerParent = getViewerParent(viewerUserId);
        UserPreferences viewerPreferences = getViewerPreferences(viewerProfile.getId());

        UserProfile candidate = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        Map<UUID, String> proposalStatusMap =
                getProposalStatusMap(viewerUserId);

        Set<UUID> savedProfileIds =
                getSavedProfileIds(viewerUserId);

        return toCard(
                viewerProfile,
                viewerParent,
                viewerPreferences,
                candidate,
                proposalStatusMap,
                savedProfileIds
        );
    }
}