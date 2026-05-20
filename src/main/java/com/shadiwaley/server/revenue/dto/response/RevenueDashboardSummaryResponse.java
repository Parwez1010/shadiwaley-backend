package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RevenueDashboardSummaryResponse {

    private BigDecimal totalRevenue;
    private BigDecimal paidRevenue;
    private BigDecimal pendingRevenue;
    private BigDecimal refundAmount;

    private long activeSubscriptions;
    private long pendingPayments;
    private long freeFamilies;
    private long paidFamilies;
}