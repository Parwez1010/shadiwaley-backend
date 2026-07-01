package com.shadiwaley.server.chat.dto.response;

import com.shadiwaley.server.chat.domain.ChatMode;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatRoomDetailResponse {

    private UUID roomId;

    private UUID rishtaRequestId;

    private UUID proposalId;

    private UUID crmCaseId;

    private UUID assignedEmployeeId;

    private String assignedEmployeeName;

    private AdminChatParticipantResponse fromProfile;

    private AdminChatParticipantResponse toProfile;

    private ChatRoomStatus status;

    private ChatMode chatMode;

    private boolean blocked;

    private boolean reported;

    private boolean needsAttention;

    private String lastReportReason;

    private String lastMessageText;

    private Instant lastMessageAt;

    private String statusLabel;
    private String expectedSpeaker;
    private Long unreadCount;
    private Long attachmentCount;


    private String lastMessageByName;

    private long messageCount;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant closedAt;
}