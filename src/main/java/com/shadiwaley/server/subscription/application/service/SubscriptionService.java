package com.shadiwaley.server.subscription.application.service;

import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.domain.PlanCatalog;
import com.shadiwaley.server.subscription.domain.PlanDefinition;
import com.shadiwaley.server.subscription.domain.PlanType;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.subscription.dto.request.SelectPlanRequest;
import com.shadiwaley.server.subscription.dto.response.PlanFeatureResponse;
import com.shadiwaley.server.subscription.dto.response.PlanResponse;
import com.shadiwaley.server.subscription.dto.response.SubscriptionResponse;
import com.shadiwaley.server.subscription.infrastructure.entity.Subscription;
import com.shadiwaley.server.subscription.infrastructure.repository.SubscriptionRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserAccountRepository userAccountRepository;

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

        return toSubscriptionResponse(subscription);
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
}