package com.shadiwaley.server.employee.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AdminLoginResponse {
    private String accessToken;
    private UUID employeeId;
    private String fullName;
    private String email;
    private String assignedDistrict;
    private EmployeeRole role;
    private boolean mustChangePassword;
    private List<String> permissions;
}