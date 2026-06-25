package com.shadiwaley.server.revenue.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.revenue.domain.*;
import com.shadiwaley.server.revenue.dto.request.AssignPlanRequest;
import com.shadiwaley.server.revenue.dto.request.ManualPaymentRequest;
import com.shadiwaley.server.revenue.dto.response.*;
import com.shadiwaley.server.revenue.infrastructure.entity.FamilySubscription;
import com.shadiwaley.server.revenue.infrastructure.entity.PaymentTransaction;
import com.shadiwaley.server.revenue.infrastructure.entity.RevenuePlan;
import com.shadiwaley.server.revenue.infrastructure.repository.FamilySubscriptionRepository;
import com.shadiwaley.server.revenue.infrastructure.repository.PaymentTransactionRepository;
import com.shadiwaley.server.revenue.infrastructure.repository.RevenuePlanRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RevenuePlanRepository revenuePlanRepository;
    private final FamilySubscriptionRepository familySubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final RevenuePermissionService revenuePermissionService;
    private final AuditLogService auditLogService;
    private final ParentProfileRepository parentProfileRepository;

    @Transactional(readOnly = true)
    public List<RevenuePlanResponse> getPlans() {
        return revenuePlanRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse assignPlan(AssignPlanRequest request) {
        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Family user not found"));

        UserProfile profile = userProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        revenuePermissionService.assertCanManageFamily(user.getId());

        RevenuePlan plan = revenuePlanRepository.findByCodeAndActiveTrue(request.getPlanCode())
                .orElseThrow(() -> new EntityNotFoundException("Active plan not found"));

        familySubscriptionRepository
                .findTopByUserAccountIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(user.getId())
                .ifPresent(existing -> {
                    existing.setCurrentSubscription(false);
                    familySubscriptionRepository.save(existing);
                });

        EmployeeAccount actor = revenuePermissionService.getCurrentEmployeeOrNull();

        FamilySubscription subscription = new FamilySubscription();
        subscription.setUserAccount(user);
        subscription.setUserProfile(profile);
        subscription.setPlan(plan);
        subscription.setPlanCode(plan.getCode());
        subscription.setPlanName(plan.getName());
        subscription.setAmount(plan.getPrice());
        subscription.setCurrency(plan.getCurrency());
        subscription.setSource(request.getSource() != null ? request.getSource() : SubscriptionSource.ADMIN_MANUAL);
        subscription.setNote(request.getNote());
        subscription.setAssignedByEmployee(actor);
        subscription.setAssignedByName(actor != null ? actor.getFullName() : "System");

        if (isFreePlan(plan)) {
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscription.setPaymentStatus(RevenuePaymentStatus.NOT_REQUIRED);
            subscription.setStartAt(Instant.now());
            subscription.setEndAt(null);
        } else {
            subscription.setSubscriptionStatus(SubscriptionStatus.PAYMENT_PENDING);
            subscription.setPaymentStatus(RevenuePaymentStatus.PENDING);
            subscription.setStartAt(null);
            subscription.setEndAt(null);
        }

        FamilySubscription saved = familySubscriptionRepository.save(subscription);

        auditLogService.record(
                AuditAction.PLAN_ASSIGNED,
                AuditEntityType.USER_PROFILE,
                profile.getId(),
                "Plan assigned: " + plan.getCode()
        );

        return toSubscriptionResponse(saved);
    }

    @Transactional
    public ManualPaymentResponse recordManualPayment(ManualPaymentRequest request) {
        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Family user not found"));

        UserProfile profile = userProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        revenuePermissionService.assertCanManageFamily(user.getId());

        FamilySubscription subscription = familySubscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));

        validateManualPayment(request, subscription);

        EmployeeAccount actor = revenuePermissionService.getCurrentEmployeeOrNull();

        PaymentTransaction payment = new PaymentTransaction();
        payment.setSubscription(subscription);
        payment.setUserAccount(user);
        payment.setUserProfile(profile);
        payment.setPlanCode(subscription.getPlanCode());
        payment.setPlanName(subscription.getPlanName());
        payment.setAmount(request.getAmount());
        payment.setCurrency(subscription.getCurrency());
        payment.setPaymentMode(request.getPaymentMode());
        payment.setPaymentStatus(request.getPaymentStatus());
        payment.setPaymentReference(request.getPaymentReference());
        payment.setPaymentNote(request.getPaymentNote());
        payment.setReceivedByEmployee(actor);
        payment.setReceivedByName(actor != null ? actor.getFullName() : "System");

        if (request.getPaymentStatus() == RevenuePaymentStatus.PAID) {
            payment.setPaidAt(Instant.now());

            subscription.setPaymentStatus(RevenuePaymentStatus.PAID);
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);

            if (subscription.getStartAt() == null) {
                subscription.setStartAt(Instant.now());
            }

            if (subscription.getPlan().getDurationDays() != null
                    && subscription.getPlan().getDurationDays() > 0) {
                subscription.setEndAt(subscription.getStartAt().plusSeconds(
                        subscription.getPlan().getDurationDays() * 86400L
                ));
            }
        }

        if (request.getPaymentStatus() == RevenuePaymentStatus.PENDING) {
            subscription.setPaymentStatus(RevenuePaymentStatus.PENDING);
            subscription.setSubscriptionStatus(SubscriptionStatus.PAYMENT_PENDING);
        }

        if (request.getPaymentStatus() == RevenuePaymentStatus.REFUNDED) {
            subscription.setPaymentStatus(RevenuePaymentStatus.REFUNDED);
            subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        }

        familySubscriptionRepository.save(subscription);
        PaymentTransaction savedPayment = paymentTransactionRepository.save(payment);

        auditLogService.record(
                AuditAction.MANUAL_PAYMENT_RECORDED,
                AuditEntityType.USER_PROFILE,
                profile.getId(),
                "Manual payment recorded: " + request.getPaymentStatus()
        );

        if (request.getPaymentStatus() == RevenuePaymentStatus.PAID) {
            auditLogService.record(
                    AuditAction.PAYMENT_MARKED_PAID,
                    AuditEntityType.USER_PROFILE,
                    profile.getId(),
                    "Payment marked paid"
            );
        }

        return ManualPaymentResponse.builder()
                .paymentId(savedPayment.getId())
                .subscriptionId(subscription.getId())
                .userId(user.getId())
                .profileId(profile.getId())
                .planCode(subscription.getPlanCode())
                .amount(savedPayment.getAmount())
                .currency(savedPayment.getCurrency())
                .paymentMode(savedPayment.getPaymentMode())
                .paymentStatus(savedPayment.getPaymentStatus())
                .paymentReference(savedPayment.getPaymentReference())
                .paidAt(savedPayment.getPaidAt())
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .build();
    }

    private void validateManualPayment(
            ManualPaymentRequest request,
            FamilySubscription subscription
    ) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount must be valid");
        }

        if (!"FREE_ONBOARDING".equals(subscription.getPlanCode())
                && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid plan requires amount greater than zero");
        }

        if (request.getPaymentStatus() == RevenuePaymentStatus.PAID
                && request.getPaymentMode() != PaymentMode.CASH
                && (request.getPaymentReference() == null || request.getPaymentReference().isBlank())) {
            throw new IllegalArgumentException("Payment reference is required for non-cash paid payments");
        }

        if (subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                && subscription.getPaymentStatus() == RevenuePaymentStatus.PAID
                && request.getPaymentStatus() == RevenuePaymentStatus.PAID) {
            throw new IllegalArgumentException("Subscription is already active and paid");
        }
    }

    private boolean isFreePlan(RevenuePlan plan) {
        return "FREE_ONBOARDING".equals(plan.getCode())
                || plan.getPrice() == null
                || plan.getPrice().compareTo(BigDecimal.ZERO) == 0;
    }

    private RevenuePlanResponse toPlanResponse(RevenuePlan plan) {
        return RevenuePlanResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .durationDays(plan.getDurationDays())
                .active(plan.isActive())
                .features(plan.getFeatures() != null
                        ? Arrays.stream(plan.getFeatures().split("\\|")).toList()
                        : List.of())
                .build();
    }

    private SubscriptionResponse toSubscriptionResponse(FamilySubscription subscription) {
        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserAccount().getId())
                .profileId(subscription.getUserProfile().getId())
                .planCode(subscription.getPlanCode())
                .planName(subscription.getPlanName())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .paymentStatus(subscription.getPaymentStatus())
                .startAt(subscription.getStartAt())
                .endAt(subscription.getEndAt())
                .assignedByName(subscription.getAssignedByName())
                .build();
    }

    @Transactional(readOnly = true)
    public FamilySubscriptionSummaryResponse getFamilySubscription(UUID userId) {
        revenuePermissionService.assertCanManageFamily(userId);

        return buildFamilySubscriptionSummary(userId);
    }

    @Transactional(readOnly = true)
    public PaymentHistoryPageResponse getFamilyPayments(
            UUID userId,
            int page,
            int size,
            RevenuePaymentStatus status,
            PaymentMode paymentMode
    ) {
        revenuePermissionService.assertCanManageFamily(userId);

        return buildFamilyPayments(userId, page, size, status, paymentMode);
    }



    private SubscriptionResponse freeSubscriptionFallback(UUID userId) {
        return SubscriptionResponse.builder()
                .subscriptionId(null)
                .userId(userId)
                .profileId(null)
                .planCode("FREE_ONBOARDING")
                .planName("Free Onboarding")
                .amount(BigDecimal.ZERO)
                .currency("INR")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .paymentStatus(RevenuePaymentStatus.NOT_REQUIRED)
                .startAt(null)
                .endAt(null)
                .assignedByName(null)
                .build();
    }

    private PaymentHistoryItemResponse toPaymentHistoryItem(PaymentTransaction payment) {
        return PaymentHistoryItemResponse.builder()
                .paymentId(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .planCode(payment.getPlanCode())
                .planName(payment.getPlanName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMode(payment.getPaymentMode())
                .paymentStatus(payment.getPaymentStatus())
                .paymentReference(payment.getPaymentReference())
                .paymentNote(payment.getPaymentNote())
                .paidAt(payment.getPaidAt())
                .receivedByName(payment.getReceivedByName())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RevenueDashboardResponse getRevenueDashboard(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        revenuePermissionService.assertCanViewDashboard();

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        BigDecimal paidRevenue = paymentTransactionRepository
                .sumAmountByStatusAndDateRange(RevenuePaymentStatus.PAID.name(), from, to);

        BigDecimal pendingRevenue = paymentTransactionRepository
                .sumAmountByStatusAndDateRange(RevenuePaymentStatus.PAID.name(), from, to);

        BigDecimal refundAmount = paymentTransactionRepository
                .sumAmountByStatusAndDateRange(RevenuePaymentStatus.PAID.name(), from, to);

        Page<PaymentTransaction> recent = paymentTransactionRepository.searchPayments(
                null,
                null,
                null,
                null,
                from,
                to,
                PageRequest.of(0, 10)
        );

        return RevenueDashboardResponse.builder()
                .summary(
                        RevenueDashboardSummaryResponse.builder()
                                .totalRevenue(paidRevenue.add(pendingRevenue))
                                .paidRevenue(paidRevenue)
                                .pendingRevenue(pendingRevenue)
                                .refundAmount(refundAmount)
                                .activeSubscriptions(
                                        familySubscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE)
                                )
                                .pendingPayments(
                                        paymentTransactionRepository.countByPaymentStatus(RevenuePaymentStatus.PENDING)
                                )
                                .freeFamilies(
                                        familySubscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE)
                                )
                                .paidFamilies(
                                        familySubscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE)
                                )
                                .build()
                )
                .planBreakdown(
                        paymentTransactionRepository.planBreakdown(from, to)
                                .stream()
                                .map(row -> RevenuePlanBreakdownResponse.builder()
                                        .planCode((String) row[0])
                                        .planName((String) row[1])
                                        .count((Long) row[2])
                                        .revenue((BigDecimal) row[3])
                                        .build())
                                .toList()
                )
                .recentPayments(
                        recent.getContent()
                                .stream()
                                .map(this::toRecentPayment)
                                .toList()
                )
                .employeeCollections(
                        paymentTransactionRepository.employeeCollections(from, to)
                                .stream()
                                .map(row -> EmployeeCollectionResponse.builder()
                                        .employeeId((UUID) row[0])
                                        .employeeName((String) row[1])
                                        .amountCollected((BigDecimal) row[2])
                                        .paymentsCount((Long) row[3])
                                        .build())
                                .toList()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public FamilySubscriptionSummaryResponse getMySubscription(UUID userId) {
        return buildFamilySubscriptionSummary(userId);
    }

    @Transactional(readOnly = true)
    public PaymentHistoryPageResponse getMyPayments(
            UUID userId,
            int page,
            int size
    ) {
        return buildFamilyPayments(userId, page, size, null, null);
    }


    @Transactional(readOnly = true)
    public RevenuePaymentListPageResponse getRevenuePayments(
            int page,
            int size,
            RevenuePaymentStatus status,
            PaymentMode paymentMode,
            String planCode,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        revenuePermissionService.assertCanViewPayments();

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );

        Page<PaymentTransaction> result = paymentTransactionRepository.searchPayments(
                status != null ? status.name() : null,
                paymentMode != null ? paymentMode.name() : null,
                isBlank(planCode) ? null : planCode,
                isBlank(search) ? null : search,
                from,
                to,
                pageable
        );

        return RevenuePaymentListPageResponse.builder()
                .payments(result.getContent().stream().map(this::toRevenuePaymentListItem).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private RevenueRecentPaymentResponse toRecentPayment(PaymentTransaction payment) {
        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(payment.getUserAccount().getId())
                .orElse(null);

        return RevenueRecentPaymentResponse.builder()
                .paymentId(payment.getId())
                .familyName(parent != null ? parent.getParentName() : null)
                .candidateName(payment.getUserProfile() != null
                        ? payment.getUserProfile().getCandidateFirstName()
                        : null)
                .planName(payment.getPlanName())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }


    private RevenuePaymentListItemResponse toRevenuePaymentListItem(PaymentTransaction payment) {
        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(payment.getUserAccount().getId())
                .orElse(null);

        return RevenuePaymentListItemResponse.builder()
                .paymentId(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .userId(payment.getUserAccount().getId())
                .profileId(payment.getUserProfile().getId())
                .familyName(parent != null ? parent.getParentName() : null)
                .candidateName(payment.getUserProfile().getCandidateFirstName())
                .phone(payment.getUserAccount().getPhone())
                .planCode(payment.getPlanCode())
                .planName(payment.getPlanName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMode(payment.getPaymentMode())
                .paymentStatus(payment.getPaymentStatus())
                .paymentReference(payment.getPaymentReference())
                .paidAt(payment.getPaidAt())
                .receivedByName(payment.getReceivedByName())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private FamilySubscriptionSummaryResponse buildFamilySubscriptionSummary(UUID userId) {
        SubscriptionResponse currentSubscription =familySubscriptionRepository
                .findTopByUserAccountIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(userId)
                .map(this::toSubscriptionResponse)
                .orElse(null);

        BigDecimal paidAmount = paymentTransactionRepository
                .sumAmountByUserAndStatus(userId, RevenuePaymentStatus.PAID);

        BigDecimal pendingAmount = paymentTransactionRepository
                .sumAmountByUserAndStatus(userId, RevenuePaymentStatus.PENDING);

        PaymentTransaction lastPayment = paymentTransactionRepository
                .findTopByUserAccountIdOrderByCreatedAtDesc(userId)
                .orElse(null);

        PaymentSummaryResponse paymentSummary = PaymentSummaryResponse.builder()
                .totalPaid(paidAmount)
                .totalPending(pendingAmount)
                .lastPaymentAt(lastPayment != null ? lastPayment.getPaidAt() : null)
                .lastPaymentMode(lastPayment != null ? lastPayment.getPaymentMode() : null)
                .lastPaymentAt(lastPayment != null ? lastPayment.getPaidAt() : null)
                .build();

        return FamilySubscriptionSummaryResponse.builder()
                .currentSubscription(currentSubscription)
                .paymentSummary(paymentSummary)
                .build();
    }

    private PaymentHistoryPageResponse buildFamilyPayments(
            UUID userId,
            int page,
            int size,
            RevenuePaymentStatus status,
            PaymentMode paymentMode
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PaymentTransaction> paymentPage;

        if (status != null && paymentMode != null) {
            paymentPage = paymentTransactionRepository
                    .findByUserAccountIdAndPaymentStatusAndPaymentModeOrderByCreatedAtDesc(
                            userId,
                            status,
                            paymentMode,
                            pageable
                    );
        } else if (status != null) {
            paymentPage = paymentTransactionRepository
                    .findByUserAccountIdAndPaymentStatusOrderByCreatedAtDesc(
                            userId,
                            status,
                            pageable
                    );
        } else if (paymentMode != null) {
            paymentPage = paymentTransactionRepository
                    .findByUserAccountIdAndPaymentModeOrderByCreatedAtDesc(
                            userId,
                            paymentMode,
                            pageable
                    );
        } else {
            paymentPage = paymentTransactionRepository
                    .findByUserAccountIdOrderByCreatedAtDesc(userId, pageable);
        }

        return PaymentHistoryPageResponse.builder()
                .payments(
                        paymentPage.getContent()
                                .stream()
                                .map(this::toPaymentHistoryItem)
                                .toList()
                )
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .last(paymentPage.isLast())
                .build();
    }




}