package com.shadiwaley.server.revenue.infrastructure.repository;

import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.revenue.infrastructure.entity.PaymentTransaction;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Page<PaymentTransaction> findByUserAccountIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<PaymentTransaction> findByUserAccountIdAndPaymentStatusOrderByCreatedAtDesc(
            UUID userId,
            RevenuePaymentStatus status,
            Pageable pageable
    );

    Optional<PaymentTransaction> findTopBySubscriptionIdAndPaymentStatusOrderByCreatedAtDesc(
            UUID subscriptionId,
            RevenuePaymentStatus status
    );


    long countByPaymentStatus(RevenuePaymentStatus status);

    Page<PaymentTransaction> findByPaymentStatusAndPaymentModeOrderByCreatedAtDesc(
            RevenuePaymentStatus status,
            PaymentMode paymentMode,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PaymentTransaction p
        WHERE p.paymentStatus = :status
        """)
    BigDecimal sumAmountByPaymentStatus(@Param("status") RevenuePaymentStatus status);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PaymentTransaction p
        WHERE p.userAccount.id = :userId
        AND p.paymentStatus = :status
        """)
    BigDecimal sumAmountByUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") RevenuePaymentStatus status
    );

    Optional<PaymentTransaction> findTopByUserAccountIdOrderByCreatedAtDesc(UUID userId);

    Page<PaymentTransaction> findByUserAccountIdAndPaymentModeOrderByCreatedAtDesc(
            UUID userId,
            PaymentMode paymentMode,
            Pageable pageable
    );

    Page<PaymentTransaction> findByUserAccountIdAndPaymentStatusAndPaymentModeOrderByCreatedAtDesc(
            UUID userId,
            RevenuePaymentStatus status,
            PaymentMode paymentMode,
            Pageable pageable
    );

    @Query(value = """
        SELECT COALESCE(SUM(amount), 0)
        FROM payment_transaction
        WHERE payment_status = CAST(:status AS varchar)
        AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))
        """, nativeQuery = true)
    BigDecimal sumAmountByStatusAndDateRange(
            @Param("status") String status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query(value = """
        SELECT pt.*
        FROM payment_transaction pt
        JOIN user_account ua ON ua.id = pt.user_account_id
        JOIN user_profile up ON up.id = pt.user_profile_id
        WHERE (:status IS NULL OR pt.payment_status = CAST(:status AS varchar))
        AND (:paymentMode IS NULL OR pt.payment_mode = CAST(:paymentMode AS varchar))
        AND (:planCode IS NULL OR LOWER(pt.plan_code) = LOWER(CAST(:planCode AS varchar)))
        AND (CAST(:fromDate AS timestamp) IS NULL OR pt.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR pt.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(pt.plan_name) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(pt.plan_code) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(pt.payment_reference, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(up.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(ua.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
        )
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM payment_transaction pt
        JOIN user_account ua ON ua.id = pt.user_account_id
        JOIN user_profile up ON up.id = pt.user_profile_id
        WHERE (:status IS NULL OR pt.payment_status = CAST(:status AS varchar))
        AND (:paymentMode IS NULL OR pt.payment_mode = CAST(:paymentMode AS varchar))
        AND (:planCode IS NULL OR LOWER(pt.plan_code) = LOWER(CAST(:planCode AS varchar)))
        AND (CAST(:fromDate AS timestamp) IS NULL OR pt.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR pt.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(pt.plan_name) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(pt.plan_code) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(pt.payment_reference, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(up.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(ua.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
        )
        """,
            nativeQuery = true)
    Page<PaymentTransaction> searchPayments(
            @Param("status") String status,
            @Param("paymentMode") String paymentMode,
            @Param("planCode") String planCode,
            @Param("search") String search,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );


    @Query(value = """
        SELECT
            pt.plan_code,
            pt.plan_name,
            COUNT(pt.id),
            COALESCE(SUM(pt.amount), 0)
        FROM payment_transaction pt
        WHERE pt.payment_status = 'PAID'
        AND (CAST(:fromDate AS timestamp) IS NULL OR pt.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR pt.created_at <= CAST(:toDate AS timestamp))
        GROUP BY pt.plan_code, pt.plan_name
        ORDER BY COALESCE(SUM(pt.amount), 0) DESC
        """, nativeQuery = true)
    List<Object[]> planBreakdown(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query(value = """
        SELECT 
            pt.received_by_employee_id,
            pt.received_by_name,
            COALESCE(SUM(pt.amount), 0),
            COUNT(pt.id)
        FROM payment_transaction pt
        WHERE pt.payment_status = 'PAID'
        AND pt.received_by_employee_id IS NOT NULL
        AND (CAST(:fromDate AS timestamp) IS NULL OR pt.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR pt.created_at <= CAST(:toDate AS timestamp))
        GROUP BY pt.received_by_employee_id, pt.received_by_name
        ORDER BY COALESCE(SUM(pt.amount), 0) DESC
        """, nativeQuery = true)
    List<Object[]> employeeCollections(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

}