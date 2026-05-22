package com.shadiwaley.server.autopilot.infrastructure.entity;

import com.shadiwaley.server.autopilot.domain.AutopilotBatchStatus;
import com.shadiwaley.server.autopilot.domain.AutopilotDispatchChannel;
import com.shadiwaley.server.autopilot.domain.AutopilotSendMode;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "autopilot_dispatch_batch")
public class AutopilotDispatchBatch {

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
    private AutopilotBatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AutopilotDispatchChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_mode", length = 50)
    private AutopilotSendMode sendMode;

    @Column(name = "share_profile_photo", nullable = false)
    private boolean shareProfilePhoto;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "items_count", nullable = false)
    private Integer itemsCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_employee_id")
    private EmployeeAccount sentByEmployee;

    @Column(name = "sent_by_name", length = 150)
    private String sentByName;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "whatsapp_template_id", length = 200)
    private String whatsappTemplateId;

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

    @Column(name = "automation_run_id", length = 150)
    private String automationRunId;

    @Column(name = "ai_suggested", nullable = false)
    private boolean aiSuggested;

    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = AutopilotBatchStatus.DRAFT;
        if (itemsCount == null) itemsCount = 0;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}