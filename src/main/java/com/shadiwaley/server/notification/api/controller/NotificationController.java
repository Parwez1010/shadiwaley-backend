package com.shadiwaley.server.notification.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.dto.response.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListResponse> getMyNotifications() {
        return ResponseFactory.success(
                "Notifications fetched successfully",
                notificationService.getMyNotifications()
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
}