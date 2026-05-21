package com.shadiwaley.server.crm.infrastructure.entity;

import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "crm_follow_up")
public class CrmFollowUp {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_case_id", nullable = false)
    private CrmCase crmCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private EmployeeAccount assignedEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CrmFollowUpStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CrmFollowUpChannel channel;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(length = 300)
    private String outcome;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (status == null) status = CrmFollowUpStatus.SCHEDULED;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}