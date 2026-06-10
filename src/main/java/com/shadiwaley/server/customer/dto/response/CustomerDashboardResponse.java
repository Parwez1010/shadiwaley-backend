package com.shadiwaley.server.customer.dto.response;

import com.shadiwaley.server.profile.dto.response.ProfileCardResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerDashboardResponse {

    private CustomerMeResponse me;

    private DashboardMetrics metrics;

    private List<ChecklistItem> profileChecklist;

    private List<DashboardActivityItem> recentActivity;

    private List<ProfileCardResponse> topMatches;

    private CustomerCrmAssignedInfoResponse crmAssignedInfo;

    @Getter
    @Builder
    public static class DashboardMetrics {
        private long profileViews;
        private long matchesCount;
        private long proposalsSent;
        private long proposalsReceived;
        private long activeChats;
        private long unreadChats;
    }

    @Getter
    @Builder
    public static class ChecklistItem {
        private String key;
        private String label;
        private boolean completed;
    }

    @Getter
    @Builder
    public static class DashboardActivityItem {
        private String type;
        private String title;
        private String description;
        private String createdAt;
    }
}