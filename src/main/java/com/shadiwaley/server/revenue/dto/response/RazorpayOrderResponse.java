package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class RazorpayOrderResponse {
    private UUID subscriptionId;
    private UUID paymentId;
    private String razorpayOrderId;
    private String razorpayKey;
    private BigDecimal amount;
    private String currency;
    private String planCode;
    private String planName;
}