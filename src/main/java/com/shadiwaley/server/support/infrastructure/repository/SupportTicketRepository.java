package com.shadiwaley.server.support.infrastructure.repository;

import com.shadiwaley.server.support.domain.SupportTicketPriority;
import com.shadiwaley.server.support.domain.SupportTicketStatus;
import com.shadiwaley.server.support.infrastructure.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID>, JpaSpecificationExecutor<SupportTicket> {

    Page<SupportTicket> findByCustomerUserIdOrderByUpdatedAtDesc(
            UUID customerUserId,
            Pageable pageable
    );

    Page<SupportTicket> findByCustomerUserIdAndStatusOrderByUpdatedAtDesc(
            UUID customerUserId,
            SupportTicketStatus status,
            Pageable pageable
    );

    Optional<SupportTicket> findByIdAndCustomerUserId(
            UUID id,
            UUID customerUserId
    );

    Page<SupportTicket> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<SupportTicket> findByStatusOrderByUpdatedAtDesc(
            SupportTicketStatus status,
            Pageable pageable
    );

    Page<SupportTicket> findByPriorityOrderByUpdatedAtDesc(
            SupportTicketPriority priority,
            Pageable pageable
    );

    Page<SupportTicket> findByAssignedEmployeeIdOrderByUpdatedAtDesc(
            UUID assignedEmployeeId,
            Pageable pageable
    );

    long countByStatus(SupportTicketStatus status);

    long countByAssignedEmployeeId(UUID employeeId);

    long countByAssignedEmployeeIdAndStatus(
            UUID employeeId,
            SupportTicketStatus status
    );

    long countByAssignedEmployeeIsNull();


    Page<SupportTicket> findByAssignedEmployeeIsNullOrderByUpdatedAtDesc(
            Pageable pageable
    );

}