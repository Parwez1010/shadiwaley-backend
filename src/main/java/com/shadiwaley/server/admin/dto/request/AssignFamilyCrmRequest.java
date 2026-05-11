package com.shadiwaley.server.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignFamilyCrmRequest {

    @NotNull(message = "Employee id is required")
    private UUID employeeId;
}