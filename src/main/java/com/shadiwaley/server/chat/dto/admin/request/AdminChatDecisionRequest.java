package com.shadiwaley.server.chat.dto.admin.request;

import com.shadiwaley.server.chat.domain.ChatFamilyDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AdminChatDecisionRequest {

    @NotNull
    private ChatFamilyDecision decision;

    private String note;

    private Instant nextFollowUpAt;
}