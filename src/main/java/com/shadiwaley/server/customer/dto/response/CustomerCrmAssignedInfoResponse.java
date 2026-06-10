package com.shadiwaley.server.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CustomerCrmAssignedInfoResponse {

    private UUID crmCaseId;

    private UUID employeeId;

    private String employeeName;

    private String employeeEmail;

    private String employeePhone;

    private String employeeRole;

    private String caseStatus;

    private String caseStage;

    private Instant nextFollowUpAt;
}