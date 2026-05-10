package com.shadiwaley.server.admin.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class EmployeePerformanceResponse {

    private UUID employeeId;

    private String employeeName;
    private String email;
    private EmployeeRole role;

    private String assignedDistrict;

    private long activeCases;
    private long scheduledFollowUps;

    private long resolvedCases;
    private long totalCases;
}