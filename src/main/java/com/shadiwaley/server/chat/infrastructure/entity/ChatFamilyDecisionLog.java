package com.shadiwaley.server.chat.infrastructure.entity;

import com.shadiwaley.server.chat.domain.ChatFamilyDecision;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chat_family_decision_log")
public class ChatFamilyDecisionLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private FamilyChatRoom room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChatFamilyDecision decision;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "next_follow_up_at")
    private Instant nextFollowUpAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private EmployeeAccount createdByEmployee;

    @Column(name = "created_by_name", length = 150)
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}