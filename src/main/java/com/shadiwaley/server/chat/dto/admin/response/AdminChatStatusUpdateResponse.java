package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatStatusUpdateResponse {

    private UUID roomId;
    private ChatRoomStatus status;
    private String statusLabel;
    private Instant updatedAt;
}