package com.shadiwaley.server.employee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ResetPasswordResponse {
    private UUID employeeId;
    private String email;
    private String temporaryPassword;
    private boolean mustChangePassword;
}