package com.shadiwaley.server.crm.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CrmDashboardResponse {

    private CrmDashboardSummaryResponse summary;

    private List<CrmDashboardFollowUpResponse> todayFollowUps;

    private List<CrmDashboardFollowUpResponse> overdueFollowUps;

    private List<CrmDashboardUnassignedCaseResponse> unassignedCases;

    private List<CrmTimelineResponse> recentActivity;

    private List<CrmEmployeeWorkloadResponse> employeeWorkload;
}