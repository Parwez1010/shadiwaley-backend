package com.shadiwaley.server.autopilot.infrastructure.repository;

import com.shadiwaley.server.autopilot.domain.AutopilotQueueStatus;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutopilotDispatchQueueRepository extends JpaRepository<AutopilotDispatchQueue, UUID> {

    @Query(
            value = """
        SELECT q.*
        FROM autopilot_dispatch_queue q
        LEFT JOIN user_profile p ON p.id = q.user_profile_id
        LEFT JOIN user_account u ON u.id = q.user_account_id
        LEFT JOIN parent_profile parent ON parent.user_account_id = q.user_account_id
        WHERE (:status IS NULL OR q.queue_status = CAST(:status AS varchar))
        AND (:assignedEmployeeId IS NULL OR q.assigned_employee_id = CAST(:assignedEmployeeId AS uuid))
        AND (
            :district IS NULL
            OR LOWER(COALESCE(parent.district, '')) = LOWER(CAST(:district AS varchar))
        )
        AND (
            :side IS NULL
            OR LOWER(CAST(u.side AS varchar)) = LOWER(CAST(:side AS varchar))
        )
        AND (
            :planCode IS NULL
            OR LOWER(COALESCE(q.plan_code, '')) = LOWER(CAST(:planCode AS varchar))
        )
        AND (
            :paymentStatus IS NULL
            OR LOWER(COALESCE(q.payment_status, '')) = LOWER(CAST(:paymentStatus AS varchar))
        )
        AND (
            :subscriptionStatus IS NULL
            OR LOWER(COALESCE(q.subscription_status, '')) = LOWER(CAST(:subscriptionStatus AS varchar))
        )
        AND (CAST(:fromDate AS timestamp) IS NULL OR q.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR q.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(COALESCE(p.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(parent.parent_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR CAST(q.id AS varchar) LIKE CONCAT('%', CAST(:search AS varchar), '%')
        )
        ORDER BY q.priority_score DESC, q.updated_at DESC
        """,
            countQuery = """
        SELECT COUNT(q.id)
        FROM autopilot_dispatch_queue q
        LEFT JOIN user_profile p ON p.id = q.user_profile_id
        LEFT JOIN user_account u ON u.id = q.user_account_id
        LEFT JOIN parent_profile parent ON parent.user_account_id = q.user_account_id
        WHERE (:status IS NULL OR q.queue_status = CAST(:status AS varchar))
        AND (:assignedEmployeeId IS NULL OR q.assigned_employee_id = CAST(:assignedEmployeeId AS uuid))
        AND (
            :district IS NULL
            OR LOWER(COALESCE(parent.district, '')) = LOWER(CAST(:district AS varchar))
        )
        AND (
            :side IS NULL
            OR LOWER(CAST(u.side AS varchar)) = LOWER(CAST(:side AS varchar))
        )
        AND (
            :planCode IS NULL
            OR LOWER(COALESCE(q.plan_code, '')) = LOWER(CAST(:planCode AS varchar))
        )
        AND (
            :paymentStatus IS NULL
            OR LOWER(COALESCE(q.payment_status, '')) = LOWER(CAST(:paymentStatus AS varchar))
        )
        AND (
            :subscriptionStatus IS NULL
            OR LOWER(COALESCE(q.subscription_status, '')) = LOWER(CAST(:subscriptionStatus AS varchar))
        )
        AND (CAST(:fromDate AS timestamp) IS NULL OR q.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR q.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(COALESCE(p.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(parent.parent_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR CAST(q.id AS varchar) LIKE CONCAT('%', CAST(:search AS varchar), '%')
        )
        """,
            nativeQuery = true
    )
    Page<AutopilotDispatchQueue> searchQueue(
            @Param("search") String search,
            @Param("status") String status,
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            @Param("district") String district,
            @Param("side") String side,
            @Param("planCode") String planCode,
            @Param("paymentStatus") String paymentStatus,
            @Param("subscriptionStatus") String subscriptionStatus,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT q.*
        FROM autopilot_dispatch_queue q
        WHERE (:assignedEmployeeId IS NULL OR q.assigned_employee_id = CAST(:assignedEmployeeId AS uuid))
        AND (CAST(:fromDate AS timestamp) IS NULL OR q.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR q.created_at <= CAST(:toDate AS timestamp))
        ORDER BY q.updated_at DESC
        """,
            nativeQuery = true
    )
    List<AutopilotDispatchQueue> findSummarySource(
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT COUNT(q)
        FROM AutopilotDispatchQueue q
        WHERE (:assignedEmployeeId IS NULL OR q.assignedEmployee.id = :assignedEmployeeId)
        AND q.lastDispatchAt >= :startOfDay
        AND q.lastDispatchAt < :endOfDay
        """)
    long countDispatchedToday(
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    @Query("""
        SELECT COUNT(q)
        FROM AutopilotDispatchQueue q
        WHERE (:assignedEmployeeId IS NULL OR q.assignedEmployee.id = :assignedEmployeeId)
        AND q.queueStatus = com.shadiwaley.server.autopilot.domain.AutopilotQueueStatus.SKIPPED
        AND q.updatedAt >= :weekStart
        """)
    long countSkippedThisWeek(
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            @Param("weekStart") Instant weekStart
    );


    boolean existsByUserProfile_Id(UUID profileId);

    Optional<AutopilotDispatchQueue> findByUserProfile_Id(UUID profileId);
}