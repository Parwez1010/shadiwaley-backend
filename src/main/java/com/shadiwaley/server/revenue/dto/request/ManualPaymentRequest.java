package com.shadiwaley.server.revenue.dto.request;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ManualPaymentRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID profileId;

    @NotNull
    private UUID subscriptionId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private PaymentMode paymentMode;

    @NotNull
    private RevenuePaymentStatus paymentStatus;

    private String paymentReference;

    private String paymentNote;
}