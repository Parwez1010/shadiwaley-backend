package com.shadiwaley.server.revenue.dto.response;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ManualPaymentResponse {

    private UUID paymentId;
    private UUID subscriptionId;
    private UUID userId;
    private UUID profileId;

    private String planCode;
    private BigDecimal amount;
    private String currency;

    private PaymentMode paymentMode;
    private RevenuePaymentStatus paymentStatus;
    private String paymentReference;

    private Instant paidAt;

    private SubscriptionStatus subscriptionStatus;
}