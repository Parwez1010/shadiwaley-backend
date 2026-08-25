package com.shadiwaley.server.revenue.dto.response;

import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionResponse {

    private UUID subscriptionId;
    private UUID userId;
    private UUID profileId;

    private String planCode;
    private String planName;
    private BigDecimal amount;
    private String currency;

    private SubscriptionStatus subscriptionStatus;
    private RevenuePaymentStatus paymentStatus;

    private Instant startAt;
    private Instant endAt;

    private String assignedByName;
}