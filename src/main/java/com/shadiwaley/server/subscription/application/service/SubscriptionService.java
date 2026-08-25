package com.shadiwaley.server.subscription.application.service;

import com.shadiwaley.server.revenue.infrastructure.entity.FamilySubscription;
import com.shadiwaley.server.revenue.infrastructure.entity.RevenuePlan;
import com.shadiwaley.server.revenue.infrastructure.repository.FamilySubscriptionRepository;
import com.shadiwaley.server.revenue.infrastructure.repository.RevenuePlanRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.domain.PlanType;
import com.shadiwaley.server.subscription.domain.SubscriptionFeature;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.subscription.dto.request.SelectPlanRequest;
import com.shadiwaley.server.subscription.dto.response.PlanFeatureResponse;
import com.shadiwaley.server.subscription.dto.response.PlanResponse;
import com.shadiwaley.server.subscription.dto.response.SubscriptionResponse;
import com.shadiwaley.server.subscription.infrastructure.entity.UserFeatureUsage;
import com.shadiwaley.server.subscription.infrastructure.repository.UserFeatureUsageRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String FREE_PLAN_CODE = "FREE_ONBOARDING";

    private final UserAccountRepository userAccountRepository;
    private final UserFeatureUsageRepository userFeatureUsageRepository;

    private final RevenuePlanRepository revenuePlanRepository;
    private final FamilySubscriptionRepository familySubscriptionRepository;


    // ============================================================
    // PLAN CATALOG
    // ============================================================

    /**
     * Returns all currently active subscription plans.
     *
     * RevenuePlan is the single source of truth for pricing.
     *
     * Pricing must NOT come from PlanCatalog or hard-coded Java code.
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans() {

        return revenuePlanRepository
                .findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toPlanResponse)
                .toList();
    }


    // ============================================================
    // CURRENT SUBSCRIPTION
    // ============================================================

    /**
     * Returns the current subscription of the authenticated user.
     *
     * Paid subscription state comes from FamilySubscription.
     *
     * If the user has no active paid subscription, the FREE plan
     * is returned as the effective plan.
     */
    @Transactional
    public SubscriptionResponse getMyPlan() {

        UUID userId = AuthUser.getCurrentUserId();

        return getCurrentSubscriptionResponse(userId);
    }


    /**
     * Internal method used by the application to resolve the
     * user's effective subscription.
     */
    @Transactional
    public SubscriptionResponse getCurrentSubscriptionResponse(UUID userId) {

        FamilySubscription subscription = findActiveSubscription(userId);

        if (subscription == null) {
            return createFreeSubscriptionResponse(userId);
        }

        if (isExpired(subscription)) {

            expireSubscription(subscription);

            return createFreeSubscriptionResponse(userId);
        }

        RevenuePlan plan = subscription.getPlan();

        if (plan == null) {
            throw new IllegalStateException(
                    "Active subscription has no associated revenue plan"
            );
        }

        return toSubscriptionResponse(subscription, plan);
    }


    /**
     * Returns the currently effective RevenuePlan.
     *
     * This method should be used by feature-gating/business logic.
     */
    @Transactional(readOnly = true)
    public RevenuePlan getCurrentPlan(UUID userId) {

        FamilySubscription subscription = findActiveSubscription(userId);

        if (subscription == null) {
            return getFreePlan();
        }

        if (isExpired(subscription)) {
            return getFreePlan();
        }

        RevenuePlan plan = subscription.getPlan();

        if (plan == null) {
            throw new IllegalStateException(
                    "Current subscription has no associated revenue plan"
            );
        }

        return plan;
    }


    // ============================================================
    // FEATURE ACCESS
    // ============================================================

    /**
     * Centralized subscription feature authorization.
     *
     * IMPORTANT:
     * The plan comes from RevenuePlan.
     * Do not use PlanCatalog here.
     */
    @Transactional
    public void validateFeatureAccess(
            UUID userId,
            SubscriptionFeature feature
    ) {

        RevenuePlan plan = getCurrentPlan(userId);

        UserFeatureUsage usage = getMonthlyUsage(userId);

        switch (feature) {

            case RISHTA_REQUEST -> {

                int monthlyLimit = plan.getRishtaRequestsPerMonth();

                if (monthlyLimit == -1) {
                    return;
                }

                if (usage.getRishtaRequestsSent() >= monthlyLimit) {

                    throw new IllegalArgumentException(
                            "Your monthly rishta request limit has been reached. " +
                                    "Please upgrade your plan."
                    );
                }
            }

            case CHAT_ROOM -> {

                /*
                 * Chat is currently allowed after proposal acceptance.
                 *
                 * The actual business rule for whether a chat room
                 * can be opened should be handled by the chat/proposal
                 * domain as well.
                 */
                if (!plan.isFamilyChat()) {

                    throw new IllegalArgumentException(
                            "Family chat is not available in your current plan."
                    );
                }
            }

            case PROFILE_VIEW -> {

                if (!plan.isBrowseProfiles()) {

                    throw new IllegalArgumentException(
                            "Profile browsing is not available in your current plan."
                    );
                }
            }

            case PRIORITY_PROFILE_REVIEW -> {

                if (!plan.isPriorityProfileReview()) {

                    throw new IllegalArgumentException(
                            "Priority profile review is not available in your current plan."
                    );
                }
            }

            case AUTOPILOT_DISPATCH -> {

                if (!plan.isAutopilotDispatch()) {

                    throw new IllegalArgumentException(
                            "Autopilot dispatch is not available in your current plan."
                    );
                }

                /*
                 * Weekly usage enforcement can be added here once
                 * autopilot usage tracking is implemented.
                 */
            }

            case DEDICATED_CRM -> {

                if (!plan.isDedicatedCrm()) {

                    throw new IllegalArgumentException(
                            "Dedicated CRM support is not available in your current plan."
                    );
                }
            }

            case MEETING_COORDINATION -> {

                if (!plan.isMeetingCoordination()) {

                    throw new IllegalArgumentException(
                            "Meeting coordination is not available in your current plan."
                    );
                }
            }
        }
    }


    // ============================================================
    // USAGE TRACKING
    // ============================================================

    @Transactional
    public void incrementRishtaUsage(UUID userId) {

        UserFeatureUsage usage = getMonthlyUsage(userId);

        usage.setRishtaRequestsSent(
                usage.getRishtaRequestsSent() + 1
        );

        userFeatureUsageRepository.save(usage);
    }


    @Transactional
    public void incrementProfileViewUsage(UUID userId) {

        UserFeatureUsage usage = getMonthlyUsage(userId);

        usage.setProfileViews(
                usage.getProfileViews() + 1
        );

        userFeatureUsageRepository.save(usage);
    }


    @Transactional
    public void incrementChatRoomUsage(UUID userId) {

        UserFeatureUsage usage = getMonthlyUsage(userId);

        usage.setActiveChatRooms(
                usage.getActiveChatRooms() + 1
        );

        userFeatureUsageRepository.save(usage);
    }


    /**
     * Gets or creates the monthly usage record.
     */
    private UserFeatureUsage getMonthlyUsage(UUID userId) {

        String usageMonth = YearMonth.now().toString();

        return userFeatureUsageRepository
                .findByUserAccountIdAndUsageMonth(
                        userId,
                        usageMonth
                )
                .orElseGet(() -> {

                    UserAccount userAccount =
                            userAccountRepository.findById(userId)
                                    .orElseThrow(() ->
                                            new EntityNotFoundException(
                                                    "User account not found"
                                            )
                                    );

                    UserFeatureUsage usage = new UserFeatureUsage();

                    usage.setUserAccount(userAccount);
                    usage.setUsageDate(LocalDate.now());
                    usage.setUsageMonth(usageMonth);

                    usage.setRishtaRequestsSent(0);
                    usage.setProfileViews(0);
                    usage.setActiveChatRooms(0);

                    return userFeatureUsageRepository.save(usage);
                });
    }


    // ============================================================
    // COMPATIBILITY API
    // ============================================================

    /**
     * LEGACY COMPATIBILITY METHOD.
     *
     * Paid subscription creation must NOT happen here anymore.
     *
     * The correct paid flow is:
     *
     * 1. POST /razorpay/order
     * 2. Razorpay checkout
     * 3. POST /razorpay/verify
     * 4. RazorpayPaymentService activates FamilySubscription
     *
     * This method is retained so existing callers do not immediately
     * break.
     *
     * Free plan can be selected directly.
     * Paid plans return PAYMENT_PENDING only as a compatibility
     * response and do NOT become active.
     */
    @Transactional
    public SubscriptionResponse selectPlan(SelectPlanRequest request) {

        UUID userId = AuthUser.getCurrentUserId();

        RevenuePlan plan = getActivePlan(
                resolvePlanCode(request.getPlanType())
        );

        if (isFreePlan(plan)) {

            return createFreeSubscriptionResponse(userId);
        }

        /*
         * IMPORTANT:
         *
         * Do not activate paid plans here.
         *
         * RazorpayPaymentService is responsible for creating the
         * FamilySubscription and activating it after successful
         * payment verification.
         */
        throw new IllegalStateException(
                "Paid plans must be purchased through the Razorpay payment flow."
        );
    }


    /**
     * LEGACY COMPATIBILITY METHOD.
     *
     * Keep this only if another module still calls requestUpgrade().
     *
     * The actual payment process should start from the Razorpay
     * order endpoint.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse requestUpgrade(
            SelectPlanRequest request
    ) {

        UUID userId = AuthUser.getCurrentUserId();

        RevenuePlan plan = getActivePlan(
                resolvePlanCode(request.getPlanType())
        );

        if (isFreePlan(plan)) {

            throw new IllegalArgumentException(
                    "Free plan is already available by default."
            );
        }

        /*
         * Do NOT create another subscription here.
         *
         * Frontend should call:
         *
         * POST /api/v1/customer/payments/razorpay/order
         *
         * with:
         *
         * {
         *     "planCode": "BASIC_299"
         * }
         */
        return createPaymentPendingResponse(
                userId,
                plan
        );
    }


    // ============================================================
    // PLAN RESOLUTION
    // ============================================================

    /**
     * Resolves the DB plan code from the legacy PlanType enum.
     *
     * This mapping is temporary and allows the old subscription API
     * to coexist while RevenuePlan becomes the source of truth.
     */
    private String resolvePlanCode(PlanType planType) {

        if (planType == null) {
            throw new IllegalArgumentException(
                    "Plan type is required"
            );
        }

        return switch (planType) {

            case FREE_ONBOARDING ->
                    "FREE_ONBOARDING";

            case SIX_MONTH_999 ->
                    "SIX_MONTH_999";

            case LIFETIME_1999 ->
                    "LIFETIME_1999";
        };
    }


    /**
     * Loads an active plan by code.
     */
    private RevenuePlan getActivePlan(String planCode) {

        return revenuePlanRepository
                .findByCodeAndActiveTrue(planCode)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Active subscription plan not found: " + planCode
                        )
                );
    }


    /**
     * Loads the free plan.
     *
     * There should be exactly one active free plan.
     */
    private RevenuePlan getFreePlan() {

        return revenuePlanRepository
                .findByCodeAndActiveTrue(FREE_PLAN_CODE)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "FREE_ONBOARDING plan is not configured"
                        )
                );
    }


    // ============================================================
    // ACTIVE SUBSCRIPTION
    // ============================================================

    private FamilySubscription findActiveSubscription(UUID userId) {

        return familySubscriptionRepository
                .findTopByUserAccountIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(
                        userId
                )
                .filter(subscription ->
                        subscription.getSubscriptionStatus()
                                == SubscriptionStatus.ACTIVE
                )
                .orElse(null);
    }


    private boolean isExpired(FamilySubscription subscription) {

        return subscription.getEndAt() != null
                && !subscription.getEndAt().isAfter(Instant.now());
    }


    /**
     * Marks an expired subscription as EXPIRED.
     *
     * The FREE plan is returned as the effective subscription.
     */
    private void expireSubscription(
            FamilySubscription subscription
    ) {

        subscription.setSubscriptionStatus(
                SubscriptionStatus.EXPIRED
        );

        subscription.setCurrentSubscription(false);

        familySubscriptionRepository.save(subscription);
    }


    // ============================================================
    // FREE PLAN RESPONSE
    // ============================================================

    private SubscriptionResponse createFreeSubscriptionResponse(
            UUID userId
    ) {

        RevenuePlan freePlan = getFreePlan();

        Instant now = Instant.now();

        return SubscriptionResponse.builder()
                .subscriptionId(null)
                .planType(resolvePlanType(freePlan.getCode()))
                .displayName(freePlan.getName())
                .status(SubscriptionStatus.ACTIVE)
                .activatedAt(now)
                .expiresAt(calculateExpiry(freePlan, now))
                .daysRemaining(
                        calculateDaysRemaining(
                                calculateExpiry(freePlan, now)
                        )
                )
                .featureMatrix(toFeatureMatrix(freePlan))
                .features(parseFeatures(freePlan.getFeatures()))
                .build();
    }


    // ============================================================
    // PAYMENT PENDING RESPONSE
    // ============================================================

    private SubscriptionResponse createPaymentPendingResponse(
            UUID userId,
            RevenuePlan plan
    ) {

        return SubscriptionResponse.builder()
                .subscriptionId(null)
                .planType(resolvePlanType(plan.getCode()))
                .displayName(plan.getName())
                .status(SubscriptionStatus.PAYMENT_PENDING)
                .activatedAt(null)
                .expiresAt(null)
                .daysRemaining(null)
                .featureMatrix(toFeatureMatrix(plan))
                .features(parseFeatures(plan.getFeatures()))
                .build();
    }


    // ============================================================
    // FAMILY SUBSCRIPTION RESPONSE
    // ============================================================

    private SubscriptionResponse toSubscriptionResponse(
            FamilySubscription subscription,
            RevenuePlan plan
    ) {

        Instant startAt = subscription.getStartAt();
        Instant endAt = subscription.getEndAt();

        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .planType(resolvePlanType(plan.getCode()))
                .displayName(plan.getName())
                .status(subscription.getSubscriptionStatus())
                .activatedAt(startAt)
                .expiresAt(endAt)
                .daysRemaining(
                        calculateDaysRemaining(endAt)
                )
                .featureMatrix(toFeatureMatrix(plan))
                .features(parseFeatures(plan.getFeatures()))
                .build();
    }


    // ============================================================
    // PLAN RESPONSE
    // ============================================================

    private PlanResponse toPlanResponse(
            RevenuePlan plan
    ) {

        return PlanResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .durationDays(plan.getDurationDays())
                .billingType(
                        plan.getBillingType() != null
                                ? plan.getBillingType().name()
                                : null
                )
                .active(plan.isActive())
                .featureMatrix(toFeatureMatrix(plan))
                .features(parseFeatures(plan.getFeatures()))
                .build();
    }


    // ============================================================
    // FEATURE MATRIX
    // ============================================================

    private List<PlanFeatureResponse> toFeatureMatrix(
            RevenuePlan plan
    ) {

        return List.of(

                feature(
                        "browseProfiles",
                        "Browse profiles",
                        plan.isBrowseProfiles()
                                ? "Unlimited"
                                : "Limited"
                ),

                feature(
                        "rishtaRequestsPerMonth",
                        "Rishta requests per month",
                        plan.getRishtaRequestsPerMonth() == -1
                                ? "Unlimited"
                                : plan.getRishtaRequestsPerMonth()
                ),

                feature(
                        "familyChat",
                        "Family chat",
                        plan.isFamilyChat()
                ),

                feature(
                        "autopilotDispatch",
                        "Autopilot dispatch",
                        plan.isAutopilotDispatch()
                ),

                feature(
                        "profilesPerWeek",
                        "Autopilot profiles per week",
                        plan.getProfilesPerWeek() == -1
                                ? "Unlimited"
                                : plan.getProfilesPerWeek()
                ),

                feature(
                        "dedicatedCrm",
                        "Dedicated CRM",
                        plan.isDedicatedCrm()
                ),

                feature(
                        "meetingCoordination",
                        "Meeting coordination",
                        plan.isMeetingCoordination()
                ),

                feature(
                        "priorityProfileReview",
                        "Priority profile review",
                        plan.isPriorityProfileReview()
                )
        );
    }


    private PlanFeatureResponse feature(
            String key,
            String label,
            Object value
    ) {

        return PlanFeatureResponse.builder()
                .key(key)
                .label(label)
                .value(value)
                .build();
    }


    // ============================================================
    // FEATURES
    // ============================================================

    private List<String> parseFeatures(String features) {

        if (features == null || features.isBlank()) {
            return List.of();
        }

        return Arrays.stream(
                        features.split("\\|")
                )
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }


    // ============================================================
    // PLAN TYPE MAPPING
    // ============================================================

    /**
     * Maps database RevenuePlan code to the existing PlanType enum.
     *
     * This is kept only because SubscriptionResponse currently
     * exposes PlanType.
     *
     * Long-term, PlanResponse/SubscriptionResponse should expose
     * planCode instead of PlanType.
     */

    private PlanType resolvePlanType(String planCode) {

        if (planCode == null) {
            throw new IllegalStateException(
                    "Plan code cannot be null"
            );
        }

        return switch (planCode) {

            case "FREE_ONBOARDING" ->
                    PlanType.FREE_ONBOARDING;

            case "SIX_MONTH_999" ->
                    PlanType.SIX_MONTH_999;

            case "LIFETIME_1999" ->
                    PlanType.LIFETIME_1999;

            default ->
                    throw new IllegalStateException(
                            "Unsupported subscription plan code: "
                                    + planCode
                    );
        };
    }


    // ============================================================
    // EXPIRY
    // ============================================================

    private Instant calculateExpiry(
            RevenuePlan plan,
            Instant activatedAt
    ) {

        if (plan.getDurationDays() == null) {
            return null;
        }

        if (plan.getDurationDays() <= 0) {
            return null;
        }

        return activatedAt.plus(
                Duration.ofDays(
                        plan.getDurationDays()
                )
        );
    }


    private Integer calculateDaysRemaining(
            Instant expiresAt
    ) {

        if (expiresAt == null) {
            return null;
        }

        long days = Duration
                .between(
                        Instant.now(),
                        expiresAt
                )
                .toDays();

        return Math.max(
                (int) days,
                0
        );
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private boolean isFreePlan(
            RevenuePlan plan
    ) {

        return FREE_PLAN_CODE.equalsIgnoreCase(
                plan.getCode()
        );
    }
}