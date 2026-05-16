package com.shadiwaley.server.crm.infrastructure.repository;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStage;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmCaseRepository extends JpaRepository<CrmCase, UUID>, JpaSpecificationExecutor<CrmCase> {

    long countByStatus(CrmCaseStatus status);

    long countByAssignedEmployeeIdAndStatus(UUID employeeId, CrmCaseStatus status);

    List<CrmCase> findTop20ByStatusOrderByUpdatedAtDesc(CrmCaseStatus status);

    Optional<CrmCase> findTopByUserAccountIdOrderByUpdatedAtDesc(UUID userAccountId);

    long countByAssignedEmployeeId(UUID assignedEmployeeId);

    long countByAssignedEmployeeIsNull();

    long countByPriority(CrmCasePriority priority);


    long countByStage(CrmCaseStage stage);

    List<CrmCase> findTop10ByAssignedEmployeeIsNullOrderByCreatedAtDesc();

    List<CrmCase> findTop10ByNextFollowUpAtBetweenOrderByNextFollowUpAtAsc(
            Instant start,
            Instant end
    );

    List<CrmCase> findTop10ByNextFollowUpAtBeforeOrderByNextFollowUpAtAsc(
            Instant instant
    );

    long countByClosedAtBetween(Instant start, Instant end);


    long countByAssignedEmployeeIdAndClosedAtBetween(
            UUID employeeId,
            Instant start,
            Instant end
    );
}