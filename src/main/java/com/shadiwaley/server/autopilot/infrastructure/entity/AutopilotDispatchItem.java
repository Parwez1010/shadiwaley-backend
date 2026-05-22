package com.shadiwaley.server.autopilot.infrastructure.entity;

import com.shadiwaley.server.autopilot.domain.AutopilotDispatchItemStatus;
import com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "autopilot_dispatch_item")
public class AutopilotDispatchItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batch_id", nullable = false)
    private AutopilotDispatchBatch dispatchBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private AutopilotDispatchQueue queue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_profile_id", nullable = false)
    private UserProfile sourceProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private UserProfile candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AutopilotDispatchItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 50)
    private AutopilotResponseStatus responseStatus;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "compatibility_score")
    private Integer compatibilityScore;

    @Column(name = "dispatch_readiness", length = 80)
    private String dispatchReadiness;

    @Column(name = "block_reason", length = 300)
    private String blockReason;

    @Column(name = "photo_included", nullable = false)
    private boolean photoIncluded;

    @Column(name = "photo_blocked_reason", length = 300)
    private String photoBlockedReason;

    @Column(name = "response_note", columnDefinition = "TEXT")
    private String responseNote;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "delivery_status", length = 80)
    private String deliveryStatus;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "raw_webhook_payload", columnDefinition = "TEXT")
    private String rawWebhookPayload;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = AutopilotDispatchItemStatus.SELECTED;
        if (responseStatus == null) responseStatus = AutopilotResponseStatus.NO_RESPONSE;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}