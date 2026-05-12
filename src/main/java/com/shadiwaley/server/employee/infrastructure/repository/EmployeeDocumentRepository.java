package com.shadiwaley.server.employee.infrastructure.repository;

import com.shadiwaley.server.employee.domain.EmployeeDocumentType;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {

    List<EmployeeDocument> findByEmployeeAccountIdAndDeletedFalseOrderByUploadedAtDesc(UUID employeeAccountId);

    Optional<EmployeeDocument> findByIdAndDeletedFalse(UUID id);

    Optional<EmployeeDocument> findTopByEmployeeAccountIdAndDocumentTypeAndDeletedFalseOrderByUploadedAtDesc(
            UUID employeeAccountId,
            EmployeeDocumentType documentType
    );
}