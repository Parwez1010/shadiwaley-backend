package com.shadiwaley.server.revenue.dto.request;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OnboardingPlanRequest {

    private String planCode;

    private String planName;

    private BigDecimal amount;

    private PaymentMode paymentMode;

    private RevenuePaymentStatus paymentStatus;

    private String paymentReference;

    private String paymentNote;
}