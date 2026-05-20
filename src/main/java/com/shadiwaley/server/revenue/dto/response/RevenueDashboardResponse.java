package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RevenueDashboardResponse {

    private RevenueDashboardSummaryResponse summary;
    private List<RevenuePlanBreakdownResponse> planBreakdown;
    private List<RevenueRecentPaymentResponse> recentPayments;
    private List<EmployeeCollectionResponse> employeeCollections;
}