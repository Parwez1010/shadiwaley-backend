package com.shadiwaley.server.chat.dto.response;

import com.shadiwaley.server.chat.domain.ChatDeliveryStatus;
import com.shadiwaley.server.chat.domain.ChatMessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatMessageResponse {

    private UUID messageId;
    private UUID roomId;

    private UUID senderUserId;
    private String senderDisplayName;

    private ChatMessageType messageType;
    private String content;

    private UUID mediaFileId;
    private String mediaType;

    private UUID replyToMessageId;
    private String replyPreview;

    private ChatDeliveryStatus deliveryStatus;

    private boolean mine;
    private boolean edited;
    private boolean deleted;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant editedAt;
    private Instant deletedAt;
}