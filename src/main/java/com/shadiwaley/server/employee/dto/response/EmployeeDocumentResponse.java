package com.shadiwaley.server.employee.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeDocumentType;
import com.shadiwaley.server.employee.domain.EmployeeDocumentVerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class EmployeeDocumentResponse {

    private UUID documentId;
    private EmployeeDocumentType documentType;
    private String originalFileName;
    private String contentType;
    private Long sizeBytes;
    private EmployeeDocumentVerificationStatus verificationStatus;
    private String rejectedReason;
    private Instant uploadedAt;
}