package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ChatMessagePageResponse {
    private List<ChatMessageResponse> messages;
    private Instant nextCursor;
    private boolean hasMore;
}