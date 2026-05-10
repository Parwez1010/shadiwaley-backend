package com.shadiwaley.server.notification.dto.response;

import com.shadiwaley.server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private UUID referenceId;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
}