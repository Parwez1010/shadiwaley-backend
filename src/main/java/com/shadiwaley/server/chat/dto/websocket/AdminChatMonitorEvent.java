package com.shadiwaley.server.chat.dto.websocket;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatMonitorEvent {

    private String event;

    private UUID roomId;

    private UUID proposalId;

    private UUID crmCaseId;

    private UUID assignedEmployeeId;

    private String title;

    private String message;

    private Instant emittedAt;
}