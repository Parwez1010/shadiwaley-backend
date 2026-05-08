package com.shadiwaley.server.subscription.dto.response;

import com.shadiwaley.server.subscription.domain.PlanType;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionResponse {
    private UUID subscriptionId;
    private PlanType planType;
    private String displayName;
    private SubscriptionStatus status;
    private Instant activatedAt;
    private Instant expiresAt;
    private Integer daysRemaining;
    private List<PlanFeatureResponse> featureMatrix;
    private List<String> features;
}