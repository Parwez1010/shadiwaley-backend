package com.shadiwaley.server.subscription.dto.request;

import com.shadiwaley.server.subscription.domain.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectPlanRequest {

    @NotNull(message = "Plan type is required")
    private PlanType planType;
}