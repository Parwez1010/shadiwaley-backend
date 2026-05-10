package com.shadiwaley.server.crm.dto.response;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmFollowUpResponse {

    private UUID followUpId;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private Instant scheduledAt;
    private Instant completedAt;
    private CrmFollowUpStatus status;
    private CrmFollowUpChannel channel;
    private String purpose;
    private String outcome;
}