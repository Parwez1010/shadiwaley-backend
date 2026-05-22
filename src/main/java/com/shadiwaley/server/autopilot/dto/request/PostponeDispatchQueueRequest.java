package com.shadiwaley.server.autopilot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PostponeDispatchQueueRequest {

    @NotNull
    private Instant nextDispatchDueAt;

    private String reason;
}