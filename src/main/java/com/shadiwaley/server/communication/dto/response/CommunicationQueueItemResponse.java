package com.shadiwaley.server.communication.dto.response;

import com.shadiwaley.server.communication.domain.CommunicationQueueType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CommunicationQueueItemResponse {

    private CommunicationQueueType queueType;

    private String itemType; // CHAT_ROOM or SUPPORT_TICKET

    private UUID itemId;

    private UUID customerUserId;

    private String customerName;

    private String customerPhone;

    private String title;

    private String subtitle;

    private String status;

    private String priority;

    private UUID assignedEmployeeId;

    private String assignedEmployeeName;

    private boolean needsAttention;

    private boolean reported;

    private String planCode;

    private String planName;

    private Instant lastActivityAt;

    private Instant createdAt;
}