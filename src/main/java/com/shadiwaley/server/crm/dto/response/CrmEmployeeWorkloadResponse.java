package com.shadiwaley.server.crm.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CrmEmployeeWorkloadResponse {

    private UUID employeeId;

    private String employeeName;

    private long openCases;

    private long dueToday;

    private long closedThisMonth;
}