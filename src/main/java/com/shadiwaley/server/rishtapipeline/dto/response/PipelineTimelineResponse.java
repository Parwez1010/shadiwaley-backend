package com.shadiwaley.server.rishtapipeline.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PipelineTimelineResponse {

    private UUID timelineId;

    private String eventType;

    private String title;

    private String description;

    private String actorName;

    private Instant createdAt;
}