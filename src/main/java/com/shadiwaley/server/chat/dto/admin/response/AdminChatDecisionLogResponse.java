package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatFamilyDecision;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatDecisionLogResponse {
    private UUID decisionId;
    private UUID roomId;
    private ChatFamilyDecision decision;
    private String note;
    private Instant nextFollowUpAt;
    private UUID createdByEmployeeId;
    private String createdByName;
    private Instant createdAt;
}