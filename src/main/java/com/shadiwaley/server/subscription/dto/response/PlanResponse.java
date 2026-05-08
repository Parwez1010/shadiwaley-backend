package com.shadiwaley.server.subscription.dto.response;

import com.shadiwaley.server.subscription.domain.PlanType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlanResponse {
    private PlanType planType;
    private String displayName;
    private int amountPaise;
    private String billingLabel;
    private int durationDays;
    private List<PlanFeatureResponse> featureMatrix;
    private List<String> features;
}