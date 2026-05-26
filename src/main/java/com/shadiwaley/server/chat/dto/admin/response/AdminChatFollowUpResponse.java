package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatFollowUpResponse {

    private UUID followUpId;
    private UUID roomId;
    private Instant scheduledAt;
    private CrmFollowUpChannel channel;
    private String purpose;
}