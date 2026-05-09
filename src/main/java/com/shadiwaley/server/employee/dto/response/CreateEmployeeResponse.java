package com.shadiwaley.server.employee.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CreateEmployeeResponse {

    private UUID employeeId;

    private String fullName;

    private String email;

    private EmployeeRole role;

    /*
     * Returned only once during employee creation.
     * Super Admin should share this securely.
     */
    private String temporaryPassword;

    private boolean mustChangePassword;
}