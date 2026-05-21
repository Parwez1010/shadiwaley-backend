package com.shadiwaley.server.rishtapipeline.dto.response;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PipelineFollowUpResponse {

    private UUID followUpId;
    private UUID proposalId;
    private UUID crmCaseId;

    private Instant scheduledAt;

    private CrmFollowUpChannel channel;
    private CrmFollowUpStatus status;

    private String purpose;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    private Instant createdAt;
}