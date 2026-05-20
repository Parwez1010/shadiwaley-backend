package com.shadiwaley.server.revenue.dto.request;

import com.shadiwaley.server.revenue.domain.SubscriptionSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignPlanRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID profileId;

    @NotBlank
    private String planCode;

    private SubscriptionSource source;

    private String note;
}