package com.shadiwaley.server.autopilot.infrastructure.entity;

import com.shadiwaley.server.autopilot.domain.AutopilotDispatchChannel;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "autopilot_dispatch_draft")
public class AutopilotDispatchDraft {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private AutopilotDispatchQueue queue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_profile_id", nullable = false)
    private UserProfile sourceProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AutopilotDispatchChannel channel;

    @Column(name = "share_profile_photo", nullable = false)
    private boolean shareProfilePhoto;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "message_preview", columnDefinition = "TEXT")
    private String messagePreview;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "can_send", nullable = false)
    private boolean canSend;

    @Column(name = "block_reason", length = 300)
    private String blockReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private EmployeeAccount createdByEmployee;

    @Column(name = "candidate_profile_ids", columnDefinition = "TEXT")
    private String candidateProfileIds;

    @Column(name = "created_by_name", length = 150)
    private String createdByName;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (expiresAt == null) expiresAt = createdAt.plusSeconds(86400);
    }
}