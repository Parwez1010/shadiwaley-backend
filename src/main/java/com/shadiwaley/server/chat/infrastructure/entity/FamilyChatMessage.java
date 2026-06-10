package com.shadiwaley.server.chat.infrastructure.entity;

import com.shadiwaley.server.chat.domain.ChatDeliveryStatus;
import com.shadiwaley.server.chat.domain.ChatMessageType;
import com.shadiwaley.server.chat.domain.ChatModerationStatus;
import com.shadiwaley.server.chat.domain.ChatSenderType;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "family_chat_message")
public class FamilyChatMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private FamilyChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private UserAccount senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private FamilyChatMessage replyToMessage;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private UserAccount deletedByUser;


    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private ChatMessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private ChatDeliveryStatus deliveryStatus;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id")
    private MediaFile mediaFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", length = 30)
    private ChatModerationStatus moderationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private ChatSenderType senderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_employee_id")
    private EmployeeAccount senderEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assisted_user_id")
    private UserAccount assistedUser;

    @Column(name = "assisted_family_name", length = 150)
    private String assistedFamilyName;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (messageType == null) {
            messageType = ChatMessageType.TEXT;
        }

        if (deliveryStatus == null) {
            deliveryStatus = ChatDeliveryStatus.SENT;
        }

        if (sentAt == null) {
            sentAt = Instant.now();
        }
        if (senderType == null) {
            senderType = ChatSenderType.CUSTOMER;
        }
        if (moderationStatus == null) {
            moderationStatus = ChatModerationStatus.CLEAN;
        }
    }
}