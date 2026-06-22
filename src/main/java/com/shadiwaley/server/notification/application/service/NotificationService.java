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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public NotificationListResponse getMyNotifications(
            int page,
            int size
    ) {

        UUID userId = AuthUser.getCurrentUserId();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        Page<UserNotification> notificationPage =
                notificationRepository.findByUserAccountIdOrderByCreatedAtDesc(
                        userId,
                        pageable
                );

        return NotificationListResponse.builder()
                .unreadCount(
                        notificationRepository.countByUserAccountIdAndReadFalse(userId)
                )
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .notifications(
                        notificationPage.getContent()
                                .stream()
                                .map(this::toResponse)
                                .toList()
                )
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
                .actionType(resolveActionType(notification))
                .actionTargetId(notification.getReferenceId())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public long getUnreadCount() {

        UUID userId = AuthUser.getCurrentUserId();

        return notificationRepository
                .countByUserAccountIdAndReadFalse(userId);
    }
    private String resolveActionType(
            UserNotification notification
    ) {

        return switch (notification.getType()) {

            case RISHTA_RECEIVED,
                 RISHTA_ACCEPTED,
                 RISHTA_REJECTED,
                 RISHTA_CANCELLED
                    -> "RISHTA";

            case CHAT_OPENED
                    -> "CHAT";

            case PROFILE_APPROVED,
                 PROFILE_REJECTED,
                 PROFILE_INCOMPLETE,
                 PROFILE_READY_FOR_REVIEW
                    -> "PROFILE";

            case MEDIA_APPROVED,
                 MEDIA_REJECTED
                    -> "MEDIA";

            default -> "SYSTEM";
        };
    }
}