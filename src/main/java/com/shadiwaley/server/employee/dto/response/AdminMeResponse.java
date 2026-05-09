package com.shadiwaley.server.employee.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AdminMeResponse {
    private UUID employeeId;
    private String fullName;
    private String email;
    private String phone;
    private EmployeeRole role;
    private EmployeeStatus status;
    private String assignedDistrict;
    private boolean mustChangePassword;
    private List<String> permissions;
}