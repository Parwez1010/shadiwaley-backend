package com.shadiwaley.server.chat.dto.admin.request;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AdminChatFollowUpRequest {

    @NotNull
    private Instant scheduledAt;

    @NotNull
    private CrmFollowUpChannel channel;

    @NotBlank
    private String purpose;
}