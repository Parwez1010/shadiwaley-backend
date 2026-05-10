package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateCrmCaseRequest {

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Case type is required")
    private CrmCaseType caseType;

    private CrmCasePriority priority = CrmCasePriority.MEDIUM;

    private UUID assignedEmployeeId;

    @Size(max = 50, message = "Source cannot exceed 50 characters")
    private String source;

    @Size(max = 300, message = "Summary cannot exceed 300 characters")
    private String summary;
}