package com.shadiwaley.server.chat.dto.response;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatRoomResponse {

    private UUID roomId;
    private UUID rishtaRequestId;

    private UUID otherUserId;
    private UUID otherProfileId;
    private String otherDisplayId;
    private String otherName;

    private ChatRoomStatus status;

    private String chatMode;

    private Boolean boyHasCrmSupport;

    private Boolean girlHasCrmSupport;

    private String expectedSpeaker;

    private String lastMessage;
    private Instant lastMessageAt;

    private long unreadCount;

    private Instant createdAt;
}