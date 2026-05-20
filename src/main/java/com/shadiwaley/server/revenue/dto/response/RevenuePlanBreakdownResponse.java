package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RevenuePlanBreakdownResponse {

    private String planCode;
    private String planName;
    private long count;
    private BigDecimal revenue;
}