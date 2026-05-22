package com.shadiwaley.server.autopilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkipDispatchQueueRequest {

    @NotBlank
    private String reason;
}