package com.shadiwaley.server.crm.dto.response;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmDashboardFollowUpResponse {

    private UUID caseId;

    private String candidateName;

    private String parentName;

    private String phone;

    private String assignedEmployeeName;

    private CrmCasePriority priority;

    private CrmCaseStage stage;

    private Instant nextFollowUpAt;
}