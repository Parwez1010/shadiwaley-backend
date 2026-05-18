package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class RescheduleFollowUpRequest {

    @NotNull
    private Instant scheduledAt;

    @NotNull
    private CrmFollowUpChannel channel;

    @Size(max = 200)
    private String purpose;

    @Size(max = 1000)
    private String note;
}