package com.shadiwaley.server.crm.infrastructure.entity;

import com.shadiwaley.server.crm.domain.CrmNoteType;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "crm_case_note")
public class CrmCaseNote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_case_id", nullable = false)
    private CrmCase crmCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private EmployeeAccount employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 50)
    private CrmNoteType noteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}