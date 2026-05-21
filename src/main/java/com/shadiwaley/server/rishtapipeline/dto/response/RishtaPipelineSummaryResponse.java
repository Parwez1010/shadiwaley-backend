package com.shadiwaley.server.rishtapipeline.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RishtaPipelineSummaryResponse {

    private long total;

    private long sent;
    private long viewed;
    private long interested;
    private long discussion;
    private long accepted;
    private long notInterested;
    private long rejected;
    private long closed;
    private long overdue;

    private BigDecimal conversionRate;
    private BigDecimal interestRate;
}