package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatReportResponse {

    private UUID reportId;

    private UUID roomId;

    private UUID messageId;

    private UUID reporterUserId;

    private String reporterName;

    private String reporterPhone;

    private String reason;

    private String details;

    private String messageContent;

    private String messageSenderName;

    private Instant createdAt;
}