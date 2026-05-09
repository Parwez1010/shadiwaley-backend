package com.shadiwaley.server.employee.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class EmployeeResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private EmployeeRole role;
    private EmployeeStatus status;
    private String assignedDistrict;
    private Integer baseSalary;
    private Integer incentivePerDispatch;
    private Integer incentivePerEngagement;
    private LocalDate joiningDate;
    private String emergencyContact;
    private String notes;
    private boolean mustChangePassword;
    private Instant passwordChangedAt;
    private Instant lastLoginAt;
    private Instant lockedUntil;
    private Instant createdAt;

    private Integer familiesAssigned;
    private Integer dispatchesThisMonth;
    private Integer engagementsThisMonth;
    private Integer incentiveEarnedThisMonth;
}