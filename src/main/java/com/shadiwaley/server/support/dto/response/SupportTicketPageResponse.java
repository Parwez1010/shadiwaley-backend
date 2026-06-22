package com.shadiwaley.server.support.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SupportTicketPageResponse {

    private List<SupportTicketResponse> items;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;
}