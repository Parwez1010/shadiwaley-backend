package com.shadiwaley.server.autopilot.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class GenerateDispatchQueueRequest {

    private String planCode;

    private UUID assignedEmployeeId;

    private Instant dueBefore;

    private Integer limit = 200;
}