package com.shadiwaley.server.notification.application.service;

import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.notification.dto.response.NotificationListResponse;
import com.shadiwaley.server.notification.dto.response.NotificationResponse;
import com.shadiwaley.server.notification.infrastructure.entity.UserNotification;
import com.shadiwaley.server.notification.infrastructure.repository.UserNotificationRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public void create(
            UUID userAccountId,
            NotificationType type,
            String title,
            String message,
            String actionUrl,
            UUID referenceId
    ) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserNotification notification = new UserNotification();
        notification.setUserAccount(userAccount);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setReferenceId(referenceId);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    public NotificationListResponse getMyNotifications() {
        UUID userId = AuthUser.getCurrentUserId();

        return NotificationListResponse.builder()
                .unreadCount(notificationRepository.countByUserAccountIdAndReadFalse(userId))
                .notifications(notificationRepository.findByUserAccountIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .map(this::toResponse)
                        .toList())
                .build();
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        UUID userId = AuthUser.getCurrentUserId();

        UserNotification notification = notificationRepository
                .findByIdAndUserAccountId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead() {
        UUID userId = AuthUser.getCurrentUserId();

        notificationRepository.findByUserAccountIdAndReadFalse(userId)
                .forEach(notification -> {
                    notification.setRead(true);
                    notification.setReadAt(Instant.now());
                    notificationRepository.save(notification);
                });
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}