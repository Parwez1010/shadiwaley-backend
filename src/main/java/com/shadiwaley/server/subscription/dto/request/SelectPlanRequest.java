package com.shadiwaley.server.subscription.dto.request;

import com.shadiwaley.server.subscription.domain.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SelectPlanRequest {

    @NotNull(message = "Plan type is required")
    private PlanType planType;
}