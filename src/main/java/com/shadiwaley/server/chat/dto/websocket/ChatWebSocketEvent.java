package com.shadiwaley.server.chat.dto.websocket;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatWebSocketEvent<T> {

    private String event;

    private UUID roomId;

    private T payload;

    private Instant emittedAt;
}