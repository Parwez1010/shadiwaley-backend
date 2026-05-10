package com.shadiwaley.server.crm.infrastructure.repository;

import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import com.shadiwaley.server.crm.infrastructure.entity.CrmFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CrmFollowUpRepository extends JpaRepository<CrmFollowUp, UUID> {

    long countByStatus(CrmFollowUpStatus status);

    long countByAssignedEmployeeIdAndStatus(UUID assignedEmployeeId, CrmFollowUpStatus status);

    List<CrmFollowUp> findByAssignedEmployeeIdAndStatusOrderByScheduledAtAsc(
            UUID assignedEmployeeId,
            CrmFollowUpStatus status
    );

    List<CrmFollowUp> findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
            CrmFollowUpStatus status,
            Instant before
    );
}