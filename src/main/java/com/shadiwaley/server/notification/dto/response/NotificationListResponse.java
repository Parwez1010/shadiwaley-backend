package com.shadiwaley.server.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationListResponse {
    private long unreadCount;
    private List<NotificationResponse> notifications;
}