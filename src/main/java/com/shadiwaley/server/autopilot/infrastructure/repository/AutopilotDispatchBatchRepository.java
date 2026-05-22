package com.shadiwaley.server.autopilot.infrastructure.repository;

import com.shadiwaley.server.autopilot.domain.AutopilotBatchStatus;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AutopilotDispatchBatchRepository
        extends JpaRepository<AutopilotDispatchBatch, UUID> {

    @Query(
            value = """
        SELECT b.*
        FROM autopilot_dispatch_batch b
        WHERE (:status IS NULL OR b.status = CAST(:status AS varchar))
        AND (CAST(:fromDate AS timestamp) IS NULL OR b.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR b.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(COALESCE(b.sent_by_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR CAST(b.id AS varchar) LIKE CONCAT('%', CAST(:search AS varchar), '%')
        )
        ORDER BY b.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(b.id)
        FROM autopilot_dispatch_batch b
        WHERE (:status IS NULL OR b.status = CAST(:status AS varchar))
        AND (CAST(:fromDate AS timestamp) IS NULL OR b.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR b.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(COALESCE(b.sent_by_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR CAST(b.id AS varchar) LIKE CONCAT('%', CAST(:search AS varchar), '%')
        )
        """,
            nativeQuery = true
    )
    Page<AutopilotDispatchBatch> searchHistory(
            @Param("search") String search,
            @Param("status") String status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}