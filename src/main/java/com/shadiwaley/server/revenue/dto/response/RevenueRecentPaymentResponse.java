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
public class RevenueRecentPaymentResponse {

    private UUID paymentId;

    private String familyName;
    private String candidateName;

    private String planName;
    private BigDecimal amount;

    private PaymentMode paymentMode;
    private RevenuePaymentStatus paymentStatus;

    private Instant paidAt;
}