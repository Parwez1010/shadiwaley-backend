package com.shadiwaley.server.employee.dto.request;

import com.shadiwaley.server.employee.domain.EmployeeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeStatusRequest {

    @NotNull(message = "Status is required")
    private EmployeeStatus status;
}