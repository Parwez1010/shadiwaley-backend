package com.shadiwaley.server.chat.infrastructure.entity;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "chat_room_internal_note",
        indexes = {
                @Index(name = "idx_chat_note_room", columnList = "room_id"),
                @Index(name = "idx_chat_note_employee", columnList = "employee_id"),
                @Index(name = "idx_chat_note_created_at", columnList = "created_at")
        }
)
public class ChatRoomInternalNote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private FamilyChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeAccount employee;

    @Column(nullable = false, length = 2000)
    private String note;

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