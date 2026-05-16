package com.shadiwaley.server.crm.infrastructure.entity;

import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "crm_case_timeline")
public class CrmCaseTimeline {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_case_id", nullable = false)
    private CrmCase crmCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private CrmTimelineEventType eventType;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_employee_id")
    private EmployeeAccount actorEmployee;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "old_value", length = 300)
    private String oldValue;

    @Column(name = "new_value", length = 300)
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}