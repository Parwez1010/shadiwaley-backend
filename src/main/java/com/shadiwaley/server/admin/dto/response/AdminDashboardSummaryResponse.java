package com.shadiwaley.server.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardSummaryResponse {
    private long totalUsers;
    private long activeUsers;

    private long incompleteProfiles;
    private long readyForReviewProfiles;
    private long liveProfiles;
    private long rejectedProfiles;

    private long pendingMediaReviews;

    private long pendingRishtaRequests;
    private long acceptedRishtaRequests;

    private long activeChatRooms;
}