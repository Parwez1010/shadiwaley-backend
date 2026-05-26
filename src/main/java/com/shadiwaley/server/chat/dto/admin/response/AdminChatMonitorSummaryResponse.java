package com.shadiwaley.server.chat.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminChatMonitorSummaryResponse {

    private long totalRooms;
    private long activeRooms;
    private long needsAttention;
    private long reportedRooms;
    private long blockedRooms;
    private long closedSuccess;
    private long closedRejected;
    private long pendingResponse;
    private long unreadMessages;
    private long todayMessages;
    private long avgResponseMinutes;
}