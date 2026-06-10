package com.shadiwaley.server.chat.dto.websocket;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PresenceEvent {

    private UUID userId;

    private boolean online;

    private Instant lastSeenAt;
}