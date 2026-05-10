package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CreateFollowUpRequest {

    @NotNull(message = "Scheduled time is required")
    private Instant scheduledAt;

    @NotNull(message = "Follow-up channel is required")
    private CrmFollowUpChannel channel;

    @NotBlank(message = "Purpose is required")
    @Size(max = 200, message = "Purpose cannot exceed 200 characters")
    private String purpose;
}