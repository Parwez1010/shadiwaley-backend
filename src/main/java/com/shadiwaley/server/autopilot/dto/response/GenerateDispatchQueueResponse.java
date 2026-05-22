package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerateDispatchQueueResponse {

    private int created;

    private int updated;

    private int skipped;

    private int blocked;
}