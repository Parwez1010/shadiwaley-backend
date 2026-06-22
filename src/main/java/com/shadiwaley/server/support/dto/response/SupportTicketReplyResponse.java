package com.shadiwaley.server.support.dto.response;

import com.shadiwaley.server.support.domain.SupportReplySenderType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SupportTicketReplyResponse {

    private UUID replyId;

    private SupportReplySenderType senderType;

    private UUID senderUserId;

    private UUID senderEmployeeId;

    private String senderName;

    private String message;

    private UUID mediaFileId;

    private String mediaUrl;

    private boolean internalNote;

    private Instant createdAt;
}