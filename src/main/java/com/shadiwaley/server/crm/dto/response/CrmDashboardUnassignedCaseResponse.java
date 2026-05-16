package com.shadiwaley.server.crm.dto.response;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmDashboardUnassignedCaseResponse {

    private UUID caseId;

    private String candidateName;

    private String parentName;

    private String phone;

    private String district;

    private CrmCasePriority priority;

    private Instant createdAt;
}