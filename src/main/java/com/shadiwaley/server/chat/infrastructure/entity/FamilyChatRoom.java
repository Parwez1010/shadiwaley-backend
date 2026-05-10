package com.shadiwaley.server.chat.infrastructure.entity;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "family_chat_room")
public class FamilyChatRoom {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rishta_request_id", nullable = false, unique = true)
    private RishtaRequest rishtaRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boy_user_id", nullable = false)
    private UserAccount boyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "girl_user_id", nullable = false)
    private UserAccount girlUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatRoomStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    private boolean blocked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_user_id")
    private UserAccount blockedByUser;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = ChatRoomStatus.ACTIVE;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}