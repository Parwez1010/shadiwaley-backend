package com.shadiwaley.server.notification.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.notification.dto.response.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListResponse> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Notifications fetched successfully",
                notificationService.getMyNotifications(page, size)
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable UUID notificationId
    ) {
        notificationService.markAsRead(notificationId);

        return ResponseFactory.success(
                "Notification marked as read",
                null
        );
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();

        return ResponseFactory.success(
                "All notifications marked as read",
                null
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {

        return ResponseFactory.success(
                "Unread count fetched successfully",
                notificationService.getUnreadCount()
        );
    }
    @GetMapping("/types")
    public ApiResponse<List<String>> getNotificationTypes() {

        return ResponseFactory.success(
                "Notification types fetched successfully",
                Arrays.stream(NotificationType.values())
                        .map(Enum::name)
                        .toList()
        );
    }
}