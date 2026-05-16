package com.shadiwaley.server.crm.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrmDashboardSummaryResponse {

    private long openCases;
    private long inProgressCases;
    private long waitingCases;
    private long urgentCases;
    private long highPriorityCases;
    private long closedThisMonth;
    private long unassignedCases;
    private long followUpsDueToday;
    private long overdueFollowUps;
    private long pendingVerification;
}