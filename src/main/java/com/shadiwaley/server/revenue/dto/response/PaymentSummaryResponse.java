package com.shadiwaley.server.revenue.dto.response;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class PaymentSummaryResponse {
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private Instant lastPaymentAt;
    private PaymentMode lastPaymentMode;
}