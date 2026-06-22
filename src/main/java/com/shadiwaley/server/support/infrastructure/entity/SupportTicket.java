package com.shadiwaley.server.support.infrastructure.entity;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.support.domain.SupportTicketCategory;
import com.shadiwaley.server.support.domain.SupportTicketPriority;
import com.shadiwaley.server.support.domain.SupportTicketSource;
import com.shadiwaley.server.support.domain.SupportTicketStatus;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "support_ticket",
        indexes = {
                @Index(name = "idx_support_ticket_customer", columnList = "customer_user_id"),
                @Index(name = "idx_support_ticket_status", columnList = "status"),
                @Index(name = "idx_support_ticket_priority", columnList = "priority"),
                @Index(name = "idx_support_ticket_assigned_employee", columnList = "assigned_employee_id"),
                @Index(name = "idx_support_ticket_created_at", columnList = "created_at")
        }
)
public class SupportTicket {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private UserAccount customerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private EmployeeAccount assignedEmployee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportTicketSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private EmployeeAccount createdByEmployee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SupportTicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportTicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportTicketPriority priority;

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_replied_at")
    private Instant lastRepliedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = SupportTicketStatus.OPEN;
        }

        if (priority == null) {
            priority = SupportTicketPriority.MEDIUM;
        }

        if (category == null) {
            category = SupportTicketCategory.OTHER;
        }
        if (source == null) {
            source = SupportTicketSource.CUSTOMER;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}