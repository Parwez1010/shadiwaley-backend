package com.shadiwaley.server.support.infrastructure.entity;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.support.domain.SupportReplySenderType;
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
        name = "support_ticket_reply",
        indexes = {
                @Index(name = "idx_support_reply_ticket", columnList = "ticket_id"),
                @Index(name = "idx_support_reply_created_at", columnList = "created_at")
        }
)
public class SupportTicketReply {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private SupportReplySenderType senderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private UserAccount senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_employee_id")
    private EmployeeAccount senderEmployee;

    @Column(nullable = false, length = 3000)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    @Column(name = "internal_note", nullable = false)
    private boolean internalNote;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}