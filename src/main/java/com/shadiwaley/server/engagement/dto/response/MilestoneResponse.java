package com.shadiwaley.server.engagement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MilestoneResponse {

    private String code;

    private String title;

    private String description;

    private boolean achieved;

    private Instant achievedAt;
}