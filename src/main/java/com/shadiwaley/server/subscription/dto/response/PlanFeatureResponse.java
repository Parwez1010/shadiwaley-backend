package com.shadiwaley.server.subscription.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanFeatureResponse {
    private String key;
    private String label;
    private Object value;
}