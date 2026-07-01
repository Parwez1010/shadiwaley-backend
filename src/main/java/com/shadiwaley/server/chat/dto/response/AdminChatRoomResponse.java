package com.shadiwaley.server.chat.dto.response;

import com.shadiwaley.server.chat.domain.ChatMode;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.domain.ChatMessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatRoomResponse {

    private UUID roomId;

    private UUID rishtaRequestId;

    private UUID proposalId;

    private UUID crmCaseId;

    private UUID assignedEmployeeId;

    private String assignedEmployeeName;

    private AdminChatParticipantResponse fromProfile;

    private AdminChatParticipantResponse toProfile;

    private UUID fromProfileId;

    private String fromCandidateName;

    private UUID toProfileId;

    private String toCandidateName;

    private ChatRoomStatus status;

    private ChatMode chatMode;

    private boolean blocked;

    private boolean reported;

    private boolean needsAttention;

    private String lastReportReason;

    private String lastMessageText;

    private ChatMessageType lastMessageType;

    private Instant lastMessageAt;

    private String lastMessageByName;

    private long messageCount;

    private String planCode;

    private String planName;

    private String subscriptionStatus;

    private boolean subscriptionActive;

    private boolean crmAssistanceAllowed;

    private String fromParentName;
    private String fromPhone;
    private String fromSide;
    private String fromDistrict;

    private String toParentName;
    private String toPhone;
    private String toSide;
    private String toDistrict;


    private boolean crmAssigned;

    private boolean canIntervene;

    private boolean canSendMessage;

    private Instant subscriptionEndAt;

    private Instant createdAt;

    private Instant updatedAt;
}