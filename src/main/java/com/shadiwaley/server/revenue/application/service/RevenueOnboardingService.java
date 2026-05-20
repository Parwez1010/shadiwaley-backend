package com.shadiwaley.server.revenue.application.service;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.revenue.domain.SubscriptionSource;
import com.shadiwaley.server.revenue.dto.request.AssignPlanRequest;
import com.shadiwaley.server.revenue.dto.request.ManualPaymentRequest;
import com.shadiwaley.server.revenue.dto.request.OnboardingPlanRequest;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevenueOnboardingService {

    private final RevenueService revenueService;

    @Transactional
    public SubscriptionResponse applyOnboardingPlan(
            UUID userId,
            UUID profileId,
            OnboardingPlanRequest planRequest,
            SubscriptionSource source
    ) {
        String planCode = planRequest != null && planRequest.getPlanCode() != null
                ? planRequest.getPlanCode()
                : "FREE_ONBOARDING";

        AssignPlanRequest assignPlanRequest = new AssignPlanRequest();
        assignPlanRequest.setUserId(userId);
        assignPlanRequest.setProfileId(profileId);
        assignPlanRequest.setPlanCode(planCode);
        assignPlanRequest.setSource(source != null ? source : SubscriptionSource.FAMILY_ONBOARDING);
        assignPlanRequest.setNote(
                planRequest != null ? planRequest.getPaymentNote() : "Default free onboarding plan"
        );

        SubscriptionResponse subscription = revenueService.assignPlan(assignPlanRequest);

        if ("FREE_ONBOARDING".equals(planCode)) {
            return subscription;
        }

        if (planRequest == null || planRequest.getPaymentStatus() == null) {
            return subscription;
        }

        if (planRequest.getPaymentStatus() == RevenuePaymentStatus.PENDING) {
            return subscription;
        }

        if (planRequest.getPaymentStatus() == RevenuePaymentStatus.PAID) {
            ManualPaymentRequest paymentRequest = new ManualPaymentRequest();
            paymentRequest.setUserId(userId);
            paymentRequest.setProfileId(profileId);
            paymentRequest.setSubscriptionId(subscription.getSubscriptionId());
            paymentRequest.setAmount(
                    planRequest.getAmount() != null
                            ? planRequest.getAmount()
                            : subscription.getAmount()
            );
            paymentRequest.setPaymentMode(
                    planRequest.getPaymentMode() != null
                            ? planRequest.getPaymentMode()
                            : PaymentMode.CASH
            );
            paymentRequest.setPaymentStatus(RevenuePaymentStatus.PAID);
            paymentRequest.setPaymentReference(planRequest.getPaymentReference());
            paymentRequest.setPaymentNote(planRequest.getPaymentNote());

            revenueService.recordManualPayment(paymentRequest);
        }

        return revenueService.getFamilySubscription(userId).getCurrentSubscription();
    }
}