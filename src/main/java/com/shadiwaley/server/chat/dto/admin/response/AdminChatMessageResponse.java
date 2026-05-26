package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatMessageType;
import com.shadiwaley.server.chat.domain.ChatModerationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatMessageResponse {

    private UUID messageId;
    private UUID roomId;

    private UUID senderUserId;
    private UUID senderProfileId;
    private String senderName;
    private String senderRole;
    private String senderSide;

    private ChatMessageType messageType;
    private String text;

    private String attachmentUrl;
    private String attachmentFileName;
    private String attachmentMimeType;
    private Long attachmentSizeBytes;

    private String direction;

    private ChatModerationStatus moderationStatus;
    private boolean reported;
    private boolean hidden;
    private boolean deleted;

    private boolean readByOtherSide;
    private Instant readAt;

    private Instant createdAt;
    private Instant updatedAt;
}