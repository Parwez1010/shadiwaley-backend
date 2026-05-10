package com.shadiwaley.server.admin.dto.response;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TodayFollowUpResponse {

    private UUID followUpId;
    private UUID caseId;

    private String employeeName;

    private String candidateName;
    private String phone;

    private CrmFollowUpChannel channel;
    private CrmFollowUpStatus status;

    private String purpose;

    private Instant scheduledAt;
}