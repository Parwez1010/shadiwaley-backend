package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatPresenceResponse {

    private UUID userId;

    private boolean online;

    private Instant lastSeenAt;
}
