package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RevenuePlanResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer durationDays;
    private boolean active;
    private List<String> features;
}