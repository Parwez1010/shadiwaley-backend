package com.shadiwaley.server.crm.dto.response;

import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmTimelineResponse {

    private UUID eventId;
    private UUID caseId;

    private CrmTimelineEventType eventType;

    private String title;
    private String description;

    private UUID actorEmployeeId;
    private String actorName;

    private String oldValue;
    private String newValue;
    private String metadata;

    private Instant createdAt;
}