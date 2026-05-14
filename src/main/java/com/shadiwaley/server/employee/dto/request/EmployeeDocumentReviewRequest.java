package com.shadiwaley.server.employee.dto.request;

import com.shadiwaley.server.employee.domain.EmployeeDocumentVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeDocumentReviewRequest {

    @NotNull(message = "Verification status is required")
    private EmployeeDocumentVerificationStatus verificationStatus;

    @Size(max = 500)
    private String rejectedReason;
}