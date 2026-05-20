package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilySubscriptionSummaryResponse {
    private SubscriptionResponse currentSubscription;
    private PaymentSummaryResponse paymentSummary;
}