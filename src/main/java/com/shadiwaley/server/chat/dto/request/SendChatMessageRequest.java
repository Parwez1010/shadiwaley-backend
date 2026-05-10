package com.shadiwaley.server.chat.dto.request;

import com.shadiwaley.server.chat.domain.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SendChatMessageRequest {

    private ChatMessageType messageType = ChatMessageType.TEXT;

    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String content;

    private UUID replyToMessageId;

    private UUID mediaFileId;
}