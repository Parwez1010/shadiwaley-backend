package com.shadiwaley.server.admin.application.service;

import com.shadiwaley.server.admin.dto.response.*;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.infrastructure.repository.FamilyChatRoomRepository;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.entity.CrmFollowUp;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmFollowUpRepository;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.revenue.application.service.RevenueService;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final MediaFileRepository mediaFileRepository;
    private final RishtaRequestRepository rishtaRequestRepository;
    private final FamilyChatRoomRepository familyChatRoomRepository;

    private final EmployeeAccountRepository employeeAccountRepository;
    private final CrmCaseRepository crmCaseRepository;
    private final CrmFollowUpRepository crmFollowUpRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final RevenueService revenueService;


    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        return AdminDashboardSummaryResponse.builder()
                .totalUsers(userAccountRepository.count())
                .activeUsers(userAccountRepository.countByAccountStatus("ACTIVE"))

                .incompleteProfiles(userProfileRepository.countByProfileStatus(ProfileStatus.INCOMPLETE))
                .readyForReviewProfiles(userProfileRepository.countByProfileStatus(ProfileStatus.READY_FOR_REVIEW))
                .liveProfiles(userProfileRepository.countByProfileStatus(ProfileStatus.LIVE))
                .rejectedProfiles(userProfileRepository.countByProfileStatus(ProfileStatus.REJECTED))

                .pendingMediaReviews(mediaFileRepository.countByReviewStatusAndDeletedFalse(MediaReviewStatus.PENDING_REVIEW))

                .pendingRishtaRequests(rishtaRequestRepository.countByStatus(RishtaRequestStatus.PENDING))
                .acceptedRishtaRequests(rishtaRequestRepository.countByStatus(RishtaRequestStatus.ACCEPTED))

                .activeChatRooms(familyChatRoomRepository.countByStatus(ChatRoomStatus.ACTIVE))
                .build();
    }

    @Transactional(readOnly = true)
    public List<PendingActionResponse> getPendingActions() {
        List<PendingActionResponse> profileActions =
                userProfileRepository.findTop20ByProfileStatusOrderByCreatedAtDesc(ProfileStatus.READY_FOR_REVIEW)
                        .stream()
                        .map(profile -> PendingActionResponse.builder()
                                .type("PROFILE_REVIEW")
                                .title("Profile ready for review")
                                .description("A family profile is ready for verification review.")
                                .priority("HIGH")
                                .referenceId(profile.getId())
                                .actionUrl("/admin/review/profile/" + profile.getId())
                                .build())
                        .toList();

        return profileActions;
    }

    @Transactional(readOnly = true)
    public Page<CrmFamilyListResponse> getFamilies(
            int page,
            int size,
            String district,
            ProfileStatus profileStatus,
            com.shadiwaley.server.user.domain.UserSide side,
            String search
    ) {
        EmployeeAccount currentEmployee = getCurrentEmployeeOrNull();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserProfile> result = userProfileRepository.findAll(
                familySpecification(
                        currentEmployee,
                        district,
                        profileStatus,
                        side,
                        search
                ),
                pageable
        );

        return result.map(this::toFamilyListResponse);
    }

    private Specification<UserProfile> familySpecification(
            EmployeeAccount currentEmployee,
            String district,
            ProfileStatus profileStatus,
            com.shadiwaley.server.user.domain.UserSide side,
            String search
    ) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicate = cb.conjunction();

            Join<UserProfile, UserAccount> accountJoin =
                    root.join("userAccount", JoinType.INNER);

            if (profileStatus != null) {
                predicate = cb.and(predicate, cb.equal(root.get("profileStatus"), profileStatus));
            }

            if (side != null) {
                predicate = cb.and(predicate, cb.equal(accountJoin.get("side"), side));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";

                predicate = cb.and(
                        predicate,
                        cb.or(
                                cb.like(cb.lower(root.get("candidateFirstName")), pattern),
                                cb.like(cb.lower(root.get("displayId")), pattern),
                                cb.like(cb.lower(accountJoin.get("phone")), pattern)
                        )
                );
            }

            if (district != null && !district.isBlank()) {
                var parentSubquery = query.subquery(UUID.class);
                var parentRoot = parentSubquery.from(ParentProfile.class);

                parentSubquery.select(parentRoot.get("userAccount").get("id"));
                parentSubquery.where(
                        cb.and(
                                cb.equal(parentRoot.get("userAccount").get("id"), accountJoin.get("id")),
                                cb.equal(cb.lower(parentRoot.get("district")), district.toLowerCase())
                        )
                );

                predicate = cb.and(predicate, cb.exists(parentSubquery));
            }

            if (currentEmployee != null && currentEmployee.getRole() == EmployeeRole.CRM_AGENT) {
                var crmCaseSubquery = query.subquery(UUID.class);
                var crmCaseRoot = crmCaseSubquery.from(CrmCase.class);

                crmCaseSubquery.select(crmCaseRoot.get("userAccount").get("id"));
                crmCaseSubquery.where(
                        cb.and(
                                cb.equal(crmCaseRoot.get("userAccount").get("id"), accountJoin.get("id")),
                                cb.equal(crmCaseRoot.get("assignedEmployee").get("id"), currentEmployee.getId())
                        )
                );

                predicate = cb.and(predicate, cb.exists(crmCaseSubquery));
            }

            return predicate;
        };
    }


    @Transactional(readOnly = true)
    public CrmFamilyDetailResponse getFamilyDetail(UUID userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        ParentProfile parent = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        UserPreferences preferences = userPreferencesRepository.findByUserProfileId(profile.getId())
                .orElse(null);
        CrmCase crmCase = crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(account.getId())
                .orElse(null);

        assertCrmAgentCanAccessFamily(crmCase);

        EmployeeAccount assignedEmployee = crmCase != null
                ? crmCase.getAssignedEmployee()
                : null;

        SubscriptionResponse subscription =
                getCurrentSubscriptionOrNull(account.getId());

        return CrmFamilyDetailResponse.builder()
                .userId(account.getId())
                .profileId(profile.getId())
                .displayId(profile.getDisplayId())

                .phone(account.getPhone())
                .side(account.getSide())
                .accountStatus(account.getAccountStatus())

                .parentName(parent.getParentName())
                .parentPhone(parent.getParentPhone())
                .parentRelation(parent.getParentRelation() != null ? parent.getParentRelation().name() : null)
                .district(parent.getDistrict())
                .state(parent.getState())
                .maslak(parent.getMaslak())
                .caste(parent.getCaste())
                .imamReference(parent.getImamReference())
                .masjidName(parent.getMasjidName())

                .candidateName(profile.getCandidateFirstName())
                .age(profile.getCandidateAge())
                .heightCm(profile.getCandidateHeightCm())
                .education(profile.getEducation())
                .quranLevel(profile.getQuranLevel())
                .namaazRegularity(profile.getNamaazRegularity())
                .previouslyMarried(profile.getPreviouslyMarried())
                .professionType(profile.getProfessionType())
                .professionTitle(profile.getProfessionTitle())
                .monthlyIncome(profile.getMonthlyIncome())
                .mehrOffered(profile.getMehrOffered())
                .houseType(profile.getHouseType())
                .familyType(profile.getFamilyType())
                .expectationsText(profile.getExpectationsText())

                .preferredMaslak(preferences != null ? preferences.getPreferredMaslak() : null)
                .preferredCaste(preferences != null ? preferences.getPreferredCaste() : null)
                .preferredState(preferences != null ? preferences.getPreferredState() : null)
                .preferredDistrict(preferences != null ? preferences.getPreferredDistrict() : null)
                .minAge(preferences != null ? preferences.getMinAge() : null)
                .maxAge(preferences != null ? preferences.getMaxAge() : null)
                .preferredEducation(preferences != null ? preferences.getPreferredEducation() : null)
                .preferredFamilyType(preferences != null ? preferences.getPreferredFamilyType() : null)
                .requireImamRef(preferences != null && preferences.isRequireImamRef())
                .requireIdVerified(preferences != null && preferences.isRequireIdVerified())

                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus())

                .assignedEmployeeId(assignedEmployee != null ? assignedEmployee.getId() : null)
                .assignedEmployeeName(assignedEmployee != null ? assignedEmployee.getFullName() : "Unassigned")

                .subscriptionId(subscription != null ? subscription.getSubscriptionId() : null)
                .planCode(subscription != null ? subscription.getPlanCode() : null)
                .planName(subscription != null ? subscription.getPlanName() : "No Active Plan")
                .planAmount(subscription != null ? subscription.getAmount() : BigDecimal.ZERO)
                .paymentStatus(subscription != null && subscription.getPaymentStatus() != null
                        ? subscription.getPaymentStatus().name()
                        : "NONE")
                .subscriptionStatus(subscription != null && subscription.getSubscriptionStatus() != null
                        ? subscription.getSubscriptionStatus().name()
                        : "NONE")

                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .lastLoginAt(account.getLastLoginAt())
                .build();
    }

    private EmployeeAccount getCurrentEmployee() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        String email = authentication.getName();

        if (email == null || email.isBlank()) {
            return null;
        }

        return employeeAccountRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);
    }


    private CrmFamilyListResponse toFamilyListResponse(UserProfile profile) {
        UserAccount account = profile.getUserAccount();

        ParentProfile parent = parentProfileRepository.findByUserAccountId(account.getId())
                .orElse(null);

        UserPreferences preferences =
                userPreferencesRepository.findByUserProfileId(profile.getId())
                        .orElse(null);

        CrmCase crmCase = crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(account.getId())
                .orElse(null);

        EmployeeAccount assignedEmployee = crmCase != null
                ? crmCase.getAssignedEmployee()
                : null;

        SubscriptionResponse subscription =
                getCurrentSubscriptionOrNull(account.getId());

        String planType;
        String planDisplayName;

        if (subscription == null) {
            planType = "NONE";
            planDisplayName = "No Active Plan";
        } else {
            planType = subscription.getPlanCode();
            planDisplayName = subscription.getPlanName();
        }


        return CrmFamilyListResponse.builder()
                .userId(account.getId())
                .profileId(profile.getId())
                .displayId(profile.getDisplayId())

                .phone(account.getPhone())
                .side(account.getSide())

                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)
                .parentRelation(parent != null && parent.getParentRelation() != null
                        ? parent.getParentRelation().name()
                        : null)
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .imamReference(parent != null ? parent.getImamReference() : null)
                .masjidName(parent != null ? parent.getMasjidName() : null)

                .candidateName(profile.getCandidateFirstName())
                .age(profile.getCandidateAge())
                .heightCm(profile.getCandidateHeightCm())
                .education(profile.getEducation())
                .quranLevel(profile.getQuranLevel())
                .namaazRegularity(profile.getNamaazRegularity())
                .previouslyMarried(profile.getPreviouslyMarried())
                .professionType(profile.getProfessionType())
                .professionTitle(profile.getProfessionTitle())
                .monthlyIncome(profile.getMonthlyIncome())
                .mehrOffered(profile.getMehrOffered())
                .houseType(profile.getHouseType())
                .familyType(profile.getFamilyType())
                .expectationsText(profile.getExpectationsText())

                .preferredMaslak(preferences != null ? preferences.getPreferredMaslak() : null)
                .preferredCaste(preferences != null ? preferences.getPreferredCaste() : null)
                .preferredState(preferences != null ? preferences.getPreferredState() : null)
                .preferredDistrict(preferences != null ? preferences.getPreferredDistrict() : null)
                .minAge(preferences != null ? preferences.getMinAge() : null)
                .maxAge(preferences != null ? preferences.getMaxAge() : null)
                .preferredEducation(preferences != null ? preferences.getPreferredEducation() : null)
                .preferredFamilyType(preferences != null ? preferences.getPreferredFamilyType() : null)
                .requireImamRef(preferences != null && preferences.isRequireImamRef())
                .requireIdVerified(preferences != null && preferences.isRequireIdVerified())

                .mode(resolveFamilyMode(planType))
                .planType(planType)
                .planDisplayName(planDisplayName)

                .profileStatus(profile.getProfileStatus())
                .displayStatus(resolveDisplayStatus(profile))
                .completionPct(profile.getCompletionPct())

                .assignedEmployeeId(assignedEmployee != null ? assignedEmployee.getId() : null)
                .assignedEmployeeName(assignedEmployee != null ? assignedEmployee.getFullName() : "Unassigned")

                .subscriptionId(subscription != null ? subscription.getSubscriptionId() : null)
                .planCode(subscription != null ? subscription.getPlanCode() : null)
                .planName(subscription != null ? subscription.getPlanName() : "No Active Plan")
                .planAmount(subscription != null ? subscription.getAmount() : BigDecimal.ZERO)
                .paymentStatus(subscription != null && subscription.getPaymentStatus() != null
                        ? subscription.getPaymentStatus().name()
                        : "NONE")
                .subscriptionStatus(subscription != null && subscription.getSubscriptionStatus() != null
                        ? subscription.getSubscriptionStatus().name()
                        : "NONE")

                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private String resolveFamilyMode(String planType) {
        if (planType == null) {
            return "SELF";
        }

        if (planType.contains("PREMIUM") || planType.contains("ELITE")) {
            return "AUTOPILOT";
        }

        return "SELF";
    }

    private String resolveDisplayStatus(UserProfile profile) {
        if (profile.getProfileStatus() == null) {
            return "UNKNOWN";
        }

        return switch (profile.getProfileStatus()) {

            case INCOMPLETE -> "Incomplete";

            case READY_FOR_REVIEW,
                 PENDING_VERIFICATION -> "Verifying";

            case LIVE,
                 VERIFIED -> "Active";

            case SUSPENDED -> "Suspended";

            case REJECTED -> "Rejected";
        };
    }


    @Transactional(readOnly = true)
    public java.util.List<EmployeePerformanceResponse> getEmployeePerformance() {

        return employeeAccountRepository.findAll()
                .stream()
                .map(employee -> EmployeePerformanceResponse.builder()
                        .employeeId(employee.getId())
                        .employeeName(employee.getFullName())
                        .email(employee.getEmail())
                        .role(employee.getRole())
                        .assignedDistrict(employee.getAssignedDistrict())

                        .activeCases(
                                crmCaseRepository.countByAssignedEmployeeIdAndStatus(
                                        employee.getId(),
                                        CrmCaseStatus.IN_PROGRESS
                                )
                        )

                        .scheduledFollowUps(
                                crmFollowUpRepository.countByAssignedEmployeeIdAndStatus(
                                        employee.getId(),
                                        CrmFollowUpStatus.SCHEDULED
                                )
                        )

                        .resolvedCases(
                                crmCaseRepository.countByAssignedEmployeeIdAndStatus(
                                        employee.getId(),
                                        CrmCaseStatus.RESOLVED
                                )
                        )

                        .totalCases(
                                crmCaseRepository.countByAssignedEmployeeIdAndStatus(
                                        employee.getId(),
                                        CrmCaseStatus.OPEN
                                )
                                        +
                                        crmCaseRepository.countByAssignedEmployeeIdAndStatus(
                                                employee.getId(),
                                                CrmCaseStatus.IN_PROGRESS
                                        )
                                        +
                                        crmCaseRepository.countByAssignedEmployeeIdAndStatus(
                                                employee.getId(),
                                                CrmCaseStatus.RESOLVED
                                        )
                        )

                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<TodayFollowUpResponse> getTodayFollowUps() {

        Instant now = Instant.now();

        Instant tomorrow = now.plusSeconds(86400);

        return crmFollowUpRepository.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                        CrmFollowUpStatus.SCHEDULED,
                        tomorrow
                )
                .stream()
                .map(this::toTodayFollowUpResponse)
                .toList();
    }

    private TodayFollowUpResponse toTodayFollowUpResponse(CrmFollowUp followUp) {

        CrmCase crmCase = followUp.getCrmCase();

        UserAccount user = crmCase.getUserAccount();

        UserProfile profile = crmCase.getUserProfile();

        EmployeeAccount employee = followUp.getAssignedEmployee();

        return TodayFollowUpResponse.builder()
                .followUpId(followUp.getId())
                .caseId(crmCase.getId())
                .employeeName(employee != null ? employee.getFullName() : null)
                .candidateName(profile.getCandidateFirstName())
                .phone(user.getPhone())
                .channel(followUp.getChannel())
                .status(followUp.getStatus())
                .purpose(followUp.getPurpose())
                .scheduledAt(followUp.getScheduledAt())
                .build();
    }
    private EmployeeAccount getCurrentEmployeeOrNull() {
        var authentication =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return null;
        }

        String principal = authentication.getName();

        try {
            UUID actorId = AuthUser.getCurrentActorId();
            var byId = employeeAccountRepository.findById(actorId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (Exception ignored) {
        }

        try {
            UUID principalId = UUID.fromString(principal);
            var byPrincipalId = employeeAccountRepository.findById(principalId);
            if (byPrincipalId.isPresent()) {
                return byPrincipalId.get();
            }
        } catch (Exception ignored) {
        }

        if (principal != null && principal.contains("@")) {
            return employeeAccountRepository
                    .findByEmailIgnoreCase(principal.trim().toLowerCase())
                    .orElse(null);
        }

        return null;
    }


    private void assertCrmAgentCanAccessFamily(CrmCase crmCase) {
        EmployeeAccount currentEmployee = getCurrentEmployeeOrNull();

        if (currentEmployee == null) {
            return;
        }

        if (currentEmployee.getRole() != EmployeeRole.CRM_AGENT) {
            return;
        }

        if (crmCase == null
                || crmCase.getAssignedEmployee() == null
                || !crmCase.getAssignedEmployee().getId().equals(currentEmployee.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can access only assigned families"
            );
        }
    }

    private SubscriptionResponse getCurrentSubscriptionOrNull(UUID userId) {
        try {
            return revenueService
                    .getFamilySubscription(userId)
                    .getCurrentSubscription();
        } catch (Exception ex) {
            return null;
        }
    }
}