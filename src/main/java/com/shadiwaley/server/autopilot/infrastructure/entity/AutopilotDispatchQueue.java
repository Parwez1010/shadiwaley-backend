package com.shadiwaley.server.autopilot.infrastructure.entity;

import com.shadiwaley.server.autopilot.domain.AutopilotQueueStatus;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
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
@Table(name = "autopilot_dispatch_queue")
public class AutopilotDispatchQueue {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_case_id")
    private CrmCase crmCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private EmployeeAccount assignedEmployee;

    @Column(name = "assigned_employee_name", length = 150)
    private String assignedEmployeeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 50)
    private AutopilotQueueStatus queueStatus;

    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore;

    @Column(length = 300)
    private String reason;

    @Column(name = "blocked_reason", length = 300)
    private String blockedReason;

    @Column(name = "plan_code", length = 80)
    private String planCode;

    @Column(name = "plan_name", length = 150)
    private String planName;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "subscription_status", length = 50)
    private String subscriptionStatus;

    @Column(name = "last_dispatch_at")
    private Instant lastDispatchAt;

    @Column(name = "next_dispatch_due_at")
    private Instant nextDispatchDueAt;

    @Column(name = "dispatch_count", nullable = false)
    private Integer dispatchCount;

    @Column(name = "pending_response_count", nullable = false)
    private Integer pendingResponseCount;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "automation_run_id", length = 150)
    private String automationRunId;

    @Column(name = "ai_suggested", nullable = false)
    private boolean aiSuggested;

    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (queueStatus == null) queueStatus = AutopilotQueueStatus.PENDING;
        if (priorityScore == null) priorityScore = 0;
        if (dispatchCount == null) dispatchCount = 0;
        if (pendingResponseCount == null) pendingResponseCount = 0;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}