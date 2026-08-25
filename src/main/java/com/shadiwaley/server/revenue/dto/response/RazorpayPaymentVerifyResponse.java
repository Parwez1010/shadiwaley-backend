package com.shadiwaley.server.revenue.dto.response;

import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RazorpayPaymentVerifyResponse {
    private UUID subscriptionId;
    private UUID paymentId;
    private RevenuePaymentStatus paymentStatus;
    private SubscriptionStatus subscriptionStatus;
}