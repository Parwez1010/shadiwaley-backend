package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class EmployeeCollectionResponse {

    private UUID employeeId;
    private String employeeName;
    private BigDecimal amountCollected;
    private long paymentsCount;
}