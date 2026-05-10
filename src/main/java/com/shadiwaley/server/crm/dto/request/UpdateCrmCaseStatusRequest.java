package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCrmCaseStatusRequest {

    @NotNull(message = "Status is required")
    private CrmCaseStatus status;

    @Size(max = 300, message = "Outcome cannot exceed 300 characters")
    private String outcome;
}