package com.shadiwaley.server.chat.dto.admin.request;

import com.shadiwaley.server.chat.domain.ChatMessageType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminSendChatMessageRequest {

    private ChatMessageType messageType = ChatMessageType.TEXT;

    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String content;

    private UUID mediaFileId;

    private UUID replyToMessageId;

    /**
     * Optional.
     * If CRM is assisting one family, frontend can pass that user's ID.
     * Message will still be shown as CRM-assisted, not fake family identity.
     */
    private UUID assistedUserId;
}