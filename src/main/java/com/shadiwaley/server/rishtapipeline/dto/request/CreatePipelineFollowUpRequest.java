package com.shadiwaley.server.rishtapipeline.dto.request;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CreatePipelineFollowUpRequest {

    @NotNull
    private Instant scheduledAt;

    @NotNull
    private CrmFollowUpChannel channel;

    private String purpose;
}