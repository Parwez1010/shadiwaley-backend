package com.shadiwaley.server.revenue.infrastructure.entity;

import com.shadiwaley.server.revenue.domain.BillingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "revenue_plan",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_revenue_plan_code",
                        columnNames = "code"
                )
        }
)
public class RevenuePlan {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Stored in major currency units.
     * Example: INR 299.00
     *
     * Razorpay amount must be converted to paise
     * at the payment boundary.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    private String currency;

    /**
     * NULL = lifetime / no expiry.
     */
    @Column(name = "duration_days")
    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false, length = 20)
    private BillingType billingType;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /**
     * Human-readable features shown on pricing UI.
     * Example:
     * Unlimited profile browsing|Family chat|CRM assistance
     */
    @Column(columnDefinition = "TEXT")
    private String features;

    // -----------------------------
    // Entitlements
    // -----------------------------

    @Column(name = "rishta_requests_per_month", nullable = false)
    private Integer rishtaRequestsPerMonth;

    @Column(name = "profiles_per_week", nullable = false)
    private Integer profilesPerWeek;

    @Column(name = "browse_profiles", nullable = false)
    private boolean browseProfiles;

    @Column(name = "family_chat", nullable = false)
    private boolean familyChat;

    @Column(name = "autopilot_dispatch", nullable = false)
    private boolean autopilotDispatch;

    @Column(name = "dedicated_crm", nullable = false)
    private boolean dedicatedCrm;

    @Column(name = "meeting_coordination", nullable = false)
    private boolean meetingCoordination;

    @Column(name = "priority_profile_review", nullable = false)
    private boolean priorityProfileReview;

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

        if (currency == null) {
            currency = "INR";
        }

        if (price == null) {
            price = BigDecimal.ZERO;
        }

        if (sortOrder == null) {
            sortOrder = 0;
        }

        if (rishtaRequestsPerMonth == null) {
            rishtaRequestsPerMonth = 0;
        }

        if (profilesPerWeek == null) {
            profilesPerWeek = 0;
        }

        if (billingType == null) {
            billingType = BillingType.ONE_TIME;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}