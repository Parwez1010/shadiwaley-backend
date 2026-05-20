package com.shadiwaley.server.revenue.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PaymentHistoryPageResponse {
    private List<PaymentHistoryItemResponse> payments;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}