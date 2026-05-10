package com.shadiwaley.server.rishta.infrastructure.entity;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rishta_request")
public class RishtaRequest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private UserAccount senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private UserAccount receiverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_profile_id", nullable = false)
    private UserProfile senderProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_profile_id", nullable = false)
    private UserProfile receiverProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RishtaRequestStatus status;

    @Column(name = "sender_note", length = 500)
    private String senderNote;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "chat_enabled", nullable = false)
    private boolean chatEnabled;

    @Column(name = "expires_at")
    private Instant expiresAt;


    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (status == null) status = RishtaRequestStatus.PENDING;
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(15L * 24 * 60 * 60);
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}