package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AdminChatMessagePageResponse {

    private List<AdminChatMessageResponse> messages;

    private Instant nextCursor;

    private boolean hasMore;
}