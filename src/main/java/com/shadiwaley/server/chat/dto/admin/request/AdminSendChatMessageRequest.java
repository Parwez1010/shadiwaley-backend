package com.shadiwaley.server.chat.dto.admin.request;

import com.shadiwaley.server.chat.domain.ChatMessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminSendChatMessageRequest {
    /**
     * Optional.
     * If CRM is assisting one family, frontend can pass that user's ID.
     * Message will still be shown as CRM-assisted, not fake family identity.
     */

    @NotNull
    private UUID assistedUserId;

    @NotNull
    private ChatMessageType messageType;

    @Size(max = 1000)
    private String content;

    private UUID mediaFileId;

    private UUID replyToMessageId;
}