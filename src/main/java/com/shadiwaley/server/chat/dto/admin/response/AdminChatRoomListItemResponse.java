package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatMessageType;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatRoomListItemResponse {

    private UUID roomId;
    private UUID proposalId;
    private UUID pipelineId;
    private UUID crmCaseId;

    private UUID fromProfileId;
    private String fromCandidateName;
    private String fromParentName;
    private String fromPhone;
    private String fromSide;
    private String fromDistrict;

    private UUID toProfileId;
    private String toCandidateName;
    private String toParentName;
    private String toPhone;
    private String toSide;
    private String toDistrict;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    private ChatRoomStatus status;
    private String statusLabel;

    private String lastMessageText;
    private ChatMessageType lastMessageType;
    private Instant lastMessageAt;
    private String lastMessageByName;

    private long unreadCount;
    private long messageCount;
    private boolean reported;
    private boolean needsAttention;
    private boolean blocked;

    private String chatMode;

    private Boolean boyHasCrmSupport;

    private Boolean girlHasCrmSupport;

    private String expectedSpeaker;

    private Integer matchScore;
    private ProposalStatus proposalStatus;
    private RishtaPipelineStage pipelineStage;
    private String pipelineStageLabel;

    private Instant createdAt;
    private Instant updatedAt;
}