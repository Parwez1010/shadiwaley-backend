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
}