package com.shadiwaley.server.subscription.infrastructure.entity;

import com.shadiwaley.server.revenue.infrastructure.entity.RevenuePlan;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_account_id",
            nullable = false
    )
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "plan_id",
            nullable = false
    )
    private RevenuePlan plan;

    /**
     * Snapshot of the commercial terms at purchase time.
     *
     * These MUST NOT change when the current plan price changes.
     */
    @Column(name = "plan_code_snapshot", nullable = false, length = 80)
    private String planCodeSnapshot;

    @Column(name = "plan_name_snapshot", nullable = false, length = 150)
    private String planNameSnapshot;

    @Column(
            name = "amount_paid",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amountPaid;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    /**
     * NULL while PAYMENT_PENDING.
     */
    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = SubscriptionStatus.PAYMENT_PENDING;
        }

        if (currency == null) {
            currency = "INR";
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}