package com.shadiwaley.server.subscription.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PlanResponse {

    private UUID id;

    private String code;

    private String name;

    private String description;

    private BigDecimal price;

    private String currency;

    private Integer durationDays;

    private String billingType;

    private boolean active;

    private List<PlanFeatureResponse> featureMatrix;

    private List<String> features;
}