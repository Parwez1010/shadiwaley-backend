package com.shadiwaley.server.communication.dto.response;

import com.shadiwaley.server.communication.domain.CommunicationActivityType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CommunicationActivityResponse {

    private UUID activityId;

    private CommunicationActivityType activityType;

    private String itemType;

    private UUID itemId;

    private UUID actorId;

    private String actorType;

    private String actorName;

    private String title;

    private String description;

    private Instant createdAt;
}