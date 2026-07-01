package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminChatDashboardResponse {

    private long totalRooms;

    private long activeRooms;

    private long blockedRooms;

    private long closedRooms;

    private long reportedRooms;

    private long needsAttentionRooms;

    private long unassignedRooms;

    private long crmAssignedRooms;

    private long totalMessages;
}