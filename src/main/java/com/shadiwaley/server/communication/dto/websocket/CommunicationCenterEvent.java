package com.shadiwaley.server.communication.dto.websocket;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CommunicationCenterEvent {

    private String event;

    private String itemType;

    private UUID itemId;

    private UUID customerUserId;

    private UUID assignedEmployeeId;

    private String title;

    private String message;

    private Instant emittedAt;
}