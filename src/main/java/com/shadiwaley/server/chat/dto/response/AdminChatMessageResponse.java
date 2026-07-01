package com.shadiwaley.server.chat.dto.response;

import com.shadiwaley.server.chat.domain.ChatDeliveryStatus;
import com.shadiwaley.server.chat.domain.ChatMessageType;
import com.shadiwaley.server.chat.domain.ChatModerationStatus;
import com.shadiwaley.server.chat.domain.ChatSenderType;
import lombok.Builder;
import lombok.Getter;
import com.shadiwaley.server.media.domain.MediaType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatMessageResponse {

    private UUID messageId;

    private UUID roomId;

    private ChatSenderType senderType;

    private UUID senderUserId;

    private String senderDisplayName;

    private UUID senderEmployeeId;

    private String senderEmployeeName;

    private UUID assistedUserId;

    private String assistedFamilyName;

    private ChatMessageType messageType;

    private String content;

    private UUID mediaFileId;

    private String mediaPreviewUrl;

    private String fileName;

    private Long fileSizeBytes;

    private String contentType;

    private UUID replyToMessageId;

    private String replyPreview;

    private ChatDeliveryStatus deliveryStatus;

    private ChatModerationStatus moderationStatus;

    private boolean edited;

    private boolean deleted;

    private MediaType mediaType;

    private Instant sentAt;

    private Instant deliveredAt;

    private Instant readAt;

    private Instant editedAt;

    private Instant deletedAt;
}