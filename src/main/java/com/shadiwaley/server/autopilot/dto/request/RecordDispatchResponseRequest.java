package com.shadiwaley.server.autopilot.dto.request;

import com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class RecordDispatchResponseRequest {

    @NotNull
    private AutopilotResponseStatus responseStatus;

    private String note;

    private Instant nextFollowUpAt;
}