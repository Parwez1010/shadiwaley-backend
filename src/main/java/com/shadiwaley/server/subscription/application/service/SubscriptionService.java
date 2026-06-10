package com.shadiwaley.server.subscription.application.service;

import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.domain.PlanCatalog;
import com.shadiwaley.server.subscription.domain.PlanDefinition;
import com.shadiwaley.server.subscription.domain.PlanType;
import com.shadiwaley.server.subscription.domain.SubscriptionFeature;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.subscription.dto.request.SelectPlanRequest;
import com.shadiwaley.server.subscription.dto.response.PlanFeatureResponse;
import com.shadiwaley.server.subscription.dto.response.PlanResponse;
import com.shadiwaley.server.subscription.dto.response.SubscriptionResponse;
import com.shadiwaley.server.subscription.infrastructure.entity.Subscription;
import com.shadiwaley.server.subscription.infrastructure.entity.UserFeatureUsage;
import com.shadiwaley.server.subscription.infrastructure.repository.SubscriptionRepository;
import com.shadiwaley.server.subscription.infrastructure.repository.UserFeatureUsageRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserFeatureUsageRepository userFeatureUsageRepository;

    public List<PlanResponse> getPlans() {
        return PlanCatalog.allPlans()
                .stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse selectPlan(SelectPlanRequest request) {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        PlanDefinition plan = PlanCatalog.getPlan(request.getPlanType());

        subscriptionRepository.findTopByUserAccountIdAndStatusOrderByCreatedAtDesc(
                userId,
                SubscriptionStatus.ACTIVE
        ).ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(existing);
        });

        Subscription subscription = new Subscription();
        subscription.setUserAccount(userAccount);
        subscription.setPlanType(plan.planType());
        subscription.setStatus(resolveInitialStatus(plan.planType()));
        subscription.setActivatedAt(Instant.now());
        subscription.setExpiresAt(resolveExpiry(plan));
        subscription.setAutoRenew(false);

        Subscription saved = subscriptionRepository.save(subscription);

        return toSubscriptionResponse(saved);
    }

    public SubscriptionResponse getMyPlan() {
        UUID userId = AuthUser.getCurrentUserId();

        Subscription subscription = subscriptionRepository
                .findTopByUserAccountIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElseGet(() -> createVirtualFreePlan(userId));

        if (subscription.getExpiresAt() != null && subscription.getExpiresAt().isBefore(Instant.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);

            Subscription freeSubscription = createVirtualFreePlan(userId);
            return toSubscriptionResponse(freeSubscription);
        }

        return toSubscriptionResponse(subscription);
    }

    @Transactional
    public void validateFeatureAccess(
            UUID userId,
            SubscriptionFeature feature
    ) {
        PlanDefinition plan = getCurrentPlanDefinition(userId);
        UserFeatureUsage usage = getMonthlyUsage(userId);

        switch (feature) {

            /*
             * Rishta requests are FREE for all registered users.
             * Subscription plans are for CRM assistance,
             * not for sending proposals.
             */
            case RISHTA_REQUEST -> {
                return;
            }

            /*
             * Family chat is also FREE.
             * Chat becomes available after proposal acceptance.
             */
            case CHAT_ROOM -> {
                return;
            }

            /*
             * Profile browsing is FREE.
             */
            case PROFILE_VIEW -> {
                return;
            }

            case PRIORITY_PROFILE_REVIEW -> {
                if (!plan.priorityProfileReview()) {
                    throw new IllegalArgumentException(
                            "Priority profile review is not available in your current plan."
                    );
                }
            }

            case AUTOPILOT_DISPATCH -> {
                if (!plan.autopilotDispatch()) {
                    throw new IllegalArgumentException(
                            "Autopilot dispatch is not available in your current plan."
                    );
                }
            }

            case DEDICATED_CRM -> {
                if (!plan.dedicatedCrm()) {
                    throw new IllegalArgumentException(
                            "Dedicated CRM support is not available in your current plan."
                    );
                }
            }

            case MEETING_COORDINATION -> {
                if (!plan.meetingCoordination()) {
                    throw new IllegalArgumentException(
                            "Meeting coordination is not available in your current plan."
                    );
                }
            }
        }
    }


    // THIS IS FOR FUTURE FUNTIONALITIES SO DON"T DELETE THIS>>>>>>>>>>>>>>>>>>>>>>>>>

//    @Transactional
//    public void validateFeatureAccess(UUID userId, SubscriptionFeature feature) {
//        PlanDefinition plan = getCurrentPlanDefinition(userId);
//        UserFeatureUsage usage = getMonthlyUsage(userId);
//
//        switch (feature) {
//            case RISHTA_REQUEST -> validateRishtaRequestAccess(plan, usage);
//
//            case CHAT_ROOM -> validateChatRoomAccess(plan, usage);
//
//            case PROFILE_VIEW -> validateProfileViewAccess(plan, usage);
//
//            case PRIORITY_PROFILE_REVIEW -> {
//                if (!plan.priorityProfileReview()) {
//                    throw new IllegalArgumentException(
//                            "Priority profile review is not available in your current plan."
//                    );
//                }
//            }
//
//            case AUTOPILOT_DISPATCH -> {
//                if (!plan.autopilotDispatch()) {
//                    throw new IllegalArgumentException(
//                            "Autopilot dispatch is not available in your current plan."
//                    );
//                }
//            }
//
//            case DEDICATED_CRM -> {
//                if (!plan.dedicatedCrm()) {
//                    throw new IllegalArgumentException(
//                            "Dedicated CRM support is not available in your current plan."
//                    );
//                }
//            }
//
//            case MEETING_COORDINATION -> {
//                if (!plan.meetingCoordination()) {
//                    throw new IllegalArgumentException(
//                            "Meeting coordination is not available in your current plan."
//                    );
//                }
//            }
//        }
//    }

    @Transactional
    public void incrementRishtaUsage(UUID userId) {
        UserFeatureUsage usage = getMonthlyUsage(userId);
        usage.setRishtaRequestsSent(usage.getRishtaRequestsSent() + 1);
        userFeatureUsageRepository.save(usage);
    }

    @Transactional
    public void incrementProfileViewUsage(UUID userId) {
        UserFeatureUsage usage = getMonthlyUsage(userId);
        usage.setProfileViews(usage.getProfileViews() + 1);
        userFeatureUsageRepository.save(usage);
    }

    @Transactional
    public void incrementChatRoomUsage(UUID userId) {
        UserFeatureUsage usage = getMonthlyUsage(userId);
        usage.setActiveChatRooms(usage.getActiveChatRooms() + 1);
        userFeatureUsageRepository.save(usage);
    }

    public PlanDefinition getCurrentPlanDefinition(UUID userId) {
        Subscription subscription = subscriptionRepository
                .findTopByUserAccountIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (subscription == null) {
            return PlanCatalog.getPlan(PlanType.FREE_ONBOARDING);
        }

        if (subscription.getExpiresAt() != null && subscription.getExpiresAt().isBefore(Instant.now())) {
            return PlanCatalog.getPlan(PlanType.FREE_ONBOARDING);
        }

        return PlanCatalog.getPlan(subscription.getPlanType());
    }

    private void validateRishtaRequestAccess(
            PlanDefinition plan,
            UserFeatureUsage usage
    ) {
        int monthlyLimit = plan.rishtaRequestsPerMonth();

        if (monthlyLimit == -1) {
            return;
        }

        if (usage.getRishtaRequestsSent() >= monthlyLimit) {
            throw new IllegalArgumentException(
                    "Your monthly rishta request limit has been reached. Please upgrade your plan."
            );
        }
    }

    private void validateChatRoomAccess(
            PlanDefinition plan,
            UserFeatureUsage usage
    ) {
        if (!plan.familyChat()) {
            throw new IllegalArgumentException(
                    "Family chat is not available in your current plan."
            );
        }
    }

    private void validateProfileViewAccess(
            PlanDefinition plan,
            UserFeatureUsage usage
    ) {
        if (!plan.browseProfiles()) {
            throw new IllegalArgumentException(
                    "Profile browsing is limited in your current plan."
            );
        }

        /*
         * If you later add profile view limits to PlanDefinition,
         * enforce them here.
         */
    }


    private Subscription createVirtualFreePlan(UUID userId) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setUserAccount(userAccount);
        subscription.setPlanType(PlanType.FREE_ONBOARDING);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setActivatedAt(Instant.now());
        subscription.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        subscription.setAutoRenew(false);

        return subscription;
    }

    private SubscriptionStatus resolveInitialStatus(PlanType planType) {
        if (planType == PlanType.FREE_ONBOARDING) {
            return SubscriptionStatus.ACTIVE;
        }

        /*
         * Payment integration is coming later.
         * For now, paid plans are marked ACTIVE so we can test downstream feature gating.
         * Later this will become PAYMENT_PENDING until Razorpay confirms payment.
         */
        return SubscriptionStatus.ACTIVE;
    }

    private Instant resolveExpiry(PlanDefinition plan) {
        if (plan.planType() == PlanType.ELITE_2499) {
            return null;
        }

        return Instant.now().plus(Duration.ofDays(plan.durationDays()));
    }

    private PlanResponse toPlanResponse(PlanDefinition plan) {
        return PlanResponse.builder()
                .planType(plan.planType())
                .displayName(plan.displayName())
                .amountPaise(plan.amountPaise())
                .billingLabel(plan.billingLabel())
                .durationDays(plan.durationDays())
                .featureMatrix(toFeatureMatrix(plan))
                .features(plan.features())
                .build();
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        PlanDefinition plan = PlanCatalog.getPlan(subscription.getPlanType());

        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .planType(subscription.getPlanType())
                .displayName(plan.displayName())
                .status(subscription.getStatus())
                .activatedAt(subscription.getActivatedAt())
                .expiresAt(subscription.getExpiresAt())
                .daysRemaining(calculateDaysRemaining(subscription.getExpiresAt()))
                .featureMatrix(toFeatureMatrix(plan))
                .features(plan.features())
                .build();
    }

    private Integer calculateDaysRemaining(Instant expiresAt) {
        if (expiresAt == null) {
            return null;
        }

        long days = Duration.between(Instant.now(), expiresAt).toDays();
        return Math.max((int) days, 0);
    }

    private List<PlanFeatureResponse> toFeatureMatrix(PlanDefinition plan) {
        return List.of(
                feature("browseProfiles", "Browse profiles", plan.browseProfiles() ? "Unlimited" : "Limited"),
                feature("rishtaRequestsPerMonth", "Rishta requests per month", plan.rishtaRequestsPerMonth() == -1 ? "Unlimited" : plan.rishtaRequestsPerMonth()),
                feature("familyChat", "Family chat", plan.familyChat()),
                feature("autopilotDispatch", "Autopilot dispatch", plan.autopilotDispatch()),
                feature("profilesPerWeek", "Autopilot profiles per week", plan.profilesPerWeek()),
                feature("dedicatedCrm", "Dedicated CRM", plan.dedicatedCrm()),
                feature("meetingCoordination", "Meeting coordination", plan.meetingCoordination()),
                feature("priorityProfileReview", "Priority profile review", plan.priorityProfileReview())
        );
    }

    private PlanFeatureResponse feature(String key, String label, Object value) {
        return PlanFeatureResponse.builder()
                .key(key)
                .label(label)
                .value(value)
                .build();
    }

    private UserFeatureUsage getMonthlyUsage(UUID userId) {
        String month = java.time.YearMonth.now().toString();

        return userFeatureUsageRepository
                .findByUserAccountIdAndUsageMonth(userId, month)
                .orElseGet(() -> {
                    UserAccount userAccount = userAccountRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User account not found"));

                    UserFeatureUsage usage = new UserFeatureUsage();
                    usage.setUserAccount(userAccount);
                    usage.setUsageDate(LocalDate.now());
                    usage.setUsageMonth(month);
                    usage.setRishtaRequestsSent(0);
                    usage.setProfileViews(0);
                    usage.setActiveChatRooms(0);

                    return userFeatureUsageRepository.save(usage);
                });
    }
}