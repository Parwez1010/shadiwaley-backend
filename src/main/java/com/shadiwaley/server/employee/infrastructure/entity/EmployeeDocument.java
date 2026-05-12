package com.shadiwaley.server.employee.infrastructure.entity;

import com.shadiwaley.server.employee.domain.EmployeeDocumentType;
import com.shadiwaley.server.employee.domain.EmployeeDocumentVerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee_document")
public class EmployeeDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_account_id", nullable = false)
    private EmployeeAccount employeeAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 60)
    private EmployeeDocumentType documentType;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 40)
    private EmployeeDocumentVerificationStatus verificationStatus;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_employee_id")
    private EmployeeAccount verifiedByEmployee;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (verificationStatus == null) {
            verificationStatus = EmployeeDocumentVerificationStatus.PENDING_REVIEW;
        }

        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}