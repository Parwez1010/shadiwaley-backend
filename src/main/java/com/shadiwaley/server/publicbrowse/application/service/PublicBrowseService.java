package com.shadiwaley.server.publicbrowse.application.service;

import com.shadiwaley.server.customer.browse.dto.response.CustomerBrowseFiltersResponse;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.publicbrowse.dto.response.PublicBrowseProfilePageResponse;
import com.shadiwaley.server.publicbrowse.dto.response.PublicBrowseProfileResponse;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicBrowseService {

    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final MediaFileRepository mediaFileRepository;

    @Transactional(readOnly = true)
    public PublicBrowseProfilePageResponse browseProfiles(
            Integer page,
            Integer size,
            String state,
            String district,
            String maslak,
            Short minAge,
            Short maxAge,
            String education,
            String familyType,
            String professionType,
            String quranLevel,
            String namaazRegularity,
            MaritalStatus maritalStatus,
            Integer minMehr,
            Integer maxMehr
    ) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserProfile> result = userProfileRepository.findAll(
                publicBrowseSpecification(
                        state,
                        district,
                        maslak,
                        minAge,
                        maxAge,
                        education,
                        familyType,
                        professionType,
                        quranLevel,
                        namaazRegularity,
                        maritalStatus,
                        minMehr,
                        maxMehr
                ),
                pageable
        );

        return PublicBrowseProfilePageResponse.builder()
                .profiles(
                        result.getContent()
                                .stream()
                                .map(this::toPublicCard)
                                .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private Specification<UserProfile> publicBrowseSpecification(
            String state,
            String district,
            String maslak,
            Short minAge,
            Short maxAge,
            String education,
            String familyType,
            String professionType,
            String quranLevel,
            String namaazRegularity,
            MaritalStatus maritalStatus,
            Integer minMehr,
            Integer maxMehr
    ) {
        return (root, query, cb) -> {
            Join<UserProfile, UserAccount> accountJoin =
                    root.join("userAccount", JoinType.INNER);

            var predicate = cb.conjunction();

            predicate = cb.and(predicate, cb.equal(root.get("profileStatus"), ProfileStatus.LIVE));

            if (minAge != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("candidateAge"), minAge));
            }

            if (maxAge != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("candidateAge"), maxAge));
            }

            if (education != null && !education.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("education")), education.toLowerCase()));
            }

            if (familyType != null && !familyType.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("familyType")), familyType.toLowerCase()));
            }

            if (professionType != null && !professionType.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("professionType")), professionType.toLowerCase()));
            }

            if (quranLevel != null && !quranLevel.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("quranLevel")), quranLevel.toLowerCase()));
            }

            if (namaazRegularity != null && !namaazRegularity.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("namaazRegularity")), namaazRegularity.toLowerCase()));
            }

            if (maritalStatus != null) {
                predicate = cb.and(predicate, cb.equal(root.get("maritalStatus"), maritalStatus));
            }

            if (minMehr != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("mehrOffered"), minMehr));
            }

            if (maxMehr != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("mehrOffered"), maxMehr));
            }

            if (hasText(state) || hasText(district) || hasText(maslak)) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<ParentProfile> parentRoot = subquery.from(ParentProfile.class);

                var parentPredicate = cb.equal(
                        parentRoot.get("userAccount").get("id"),
                        accountJoin.get("id")
                );

                if (hasText(state)) {
                    parentPredicate = cb.and(
                            parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("state")), state.toLowerCase())
                    );
                }

                if (hasText(district)) {
                    parentPredicate = cb.and(
                            parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("district")), district.toLowerCase())
                    );
                }

                if (hasText(maslak)) {
                    parentPredicate = cb.and(
                            parentPredicate,
                            cb.equal(cb.lower(parentRoot.get("maslak")), maslak.toLowerCase())
                    );
                }

                subquery.select(parentRoot.get("userAccount").get("id"))
                        .where(parentPredicate);

                predicate = cb.and(predicate, cb.exists(subquery));
            }

            return predicate;
        };
    }

    private PublicBrowseProfileResponse toPublicCard(UserProfile profile) {
        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(profile.getUserAccount().getId())
                .orElse(null);

        boolean hasApprovedPhoto = hasApprovedProfilePhoto(profile.getId());

        return PublicBrowseProfileResponse.builder()
                .profileId(profile.getId())
                .displayId(profile.getDisplayId())
                .side(profile.getUserAccount().getSide())

                .candidateAge(profile.getCandidateAge())

                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .maslak(parent != null ? parent.getMaslak() : null)

                .education(profile.getEducation())
                .quranLevel(profile.getQuranLevel())
                .namaazRegularity(profile.getNamaazRegularity())
                .professionType(profile.getProfessionType())
                .familyType(profile.getFamilyType())

                .mehrOffered(profile.getMehrOffered())
                .mehrMinimumExpected(profile.getMehrMinimumExpected())
                .expectationsText(profile.getExpectationsText())

                .hasApprovedPhoto(hasApprovedPhoto)

                .saved(false)
                .proposalStatus(null)

                .match(
                        PublicBrowseProfileResponse.PublicMatchResponse.builder()
                                .totalScore(null)
                                .build()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerBrowseFiltersResponse getFilters() {
        return CustomerBrowseFiltersResponse.builder()
                .states(List.of("Bihar", "Jharkhand", "Uttar Pradesh", "Delhi", "West Bengal"))
                .districts(List.of("Patna", "Gaya", "Muzaffarpur", "Bhagalpur", "Darbhanga"))
                .maslak(List.of("Sunni", "Deobandi", "Barelvi", "Ahl-e-Hadith"))
                .education(List.of("Matric", "Intermediate", "Graduate", "Post Graduate", "B.Tech", "MBBS", "MBA"))
                .familyTypes(List.of("NUCLEAR", "JOINT"))
                .professionTypes(List.of("PRIVATE_JOB", "GOVERNMENT_JOB", "BUSINESS", "SELF_EMPLOYED", "STUDENT"))
                .quranLevels(List.of("CAN_READ", "HIFZ", "LEARNING", "BASIC"))
                .namaazRegularity(List.of("FIVE_TIMES", "SOMETIMES", "FRIDAY_ONLY", "LEARNING"))
                .maritalStatuses(
                        Arrays.stream(MaritalStatus.values())
                                .map(Enum::name)
                                .toList()
                )
                .minAge((short) 18)
                .maxAge((short) 60)
                .minIncome(0)
                .maxIncome(500000)
                .minMehr(0)
                .maxMehr(1000000)
                .build();
    }

    private boolean hasApprovedProfilePhoto(UUID profileId) {
        return mediaFileRepository
                .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                        profileId,
                        MediaType.PROFILE_PHOTO
                )
                .filter(media -> media.getReviewStatus() == MediaReviewStatus.APPROVED)
                .isPresent();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}