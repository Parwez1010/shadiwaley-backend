package com.shadiwaley.server.revenue.dto.response;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RevenuePaymentListItemResponse {

    private UUID paymentId;
    private UUID subscriptionId;

    private UUID userId;
    private UUID profileId;

    private String familyName;
    private String candidateName;
    private String phone;

    private String planCode;
    private String planName;

    private BigDecimal amount;
    private String currency;

    private PaymentMode paymentMode;
    private RevenuePaymentStatus paymentStatus;

    private String paymentReference;
    private Instant paidAt;

    private String receivedByName;
    private Instant createdAt;
}