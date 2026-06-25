package com.shadiwaley.server.revenue.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VerifyPaymentRequest {

    private UUID paymentId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;
}
