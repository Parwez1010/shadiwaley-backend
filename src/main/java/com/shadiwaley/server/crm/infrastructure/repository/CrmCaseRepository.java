package com.shadiwaley.server.crm.infrastructure.repository;

import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmCaseRepository extends JpaRepository<CrmCase, UUID>, JpaSpecificationExecutor<CrmCase> {

    long countByStatus(CrmCaseStatus status);

    long countByAssignedEmployeeIdAndStatus(UUID employeeId, CrmCaseStatus status);

    List<CrmCase> findTop20ByStatusOrderByUpdatedAtDesc(CrmCaseStatus status);

    Optional<CrmCase> findTopByUserAccountIdOrderByUpdatedAtDesc(UUID userAccountId);

    long countByAssignedEmployeeId(UUID assignedEmployeeId);
}