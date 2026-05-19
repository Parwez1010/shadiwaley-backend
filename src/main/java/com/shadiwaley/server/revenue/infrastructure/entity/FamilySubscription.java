package com.shadiwaley.server.revenue.infrastructure.entity;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.revenue.domain.SubscriptionSource;
import com.shadiwaley.server.revenue.domain.SubscriptionStatus;
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
@Table(name = "family_subscription")
public class FamilySubscription {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private RevenuePlan plan;

    @Column(name = "plan_code", nullable = false, length = 80)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 150)
    private String planName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 50)
    private SubscriptionStatus subscriptionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private RevenuePaymentStatus paymentStatus;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "current_subscription", nullable = false)
    private boolean currentSubscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_employee_id")
    private EmployeeAccount assignedByEmployee;

    @Column(name = "assigned_by_name", length = 150)
    private String assignedByName;

    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private SubscriptionSource source;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (currency == null) currency = "INR";
        if (amount == null) amount = BigDecimal.ZERO;
        currentSubscription = true;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}