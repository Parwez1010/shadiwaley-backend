package com.shadiwaley.server.chat.dto.admin.request;

import com.shadiwaley.server.chat.domain.ChatModerationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMessageModerationRequest {

    @NotNull
    private ChatModerationStatus moderationStatus;

    private String reason;
}