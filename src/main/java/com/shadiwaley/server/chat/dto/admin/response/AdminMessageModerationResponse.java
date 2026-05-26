package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatModerationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminMessageModerationResponse {

    private UUID messageId;
    private ChatModerationStatus moderationStatus;
    private Instant updatedAt;
}