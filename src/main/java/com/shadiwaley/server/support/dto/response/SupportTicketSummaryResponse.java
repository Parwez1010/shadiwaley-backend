package com.shadiwaley.server.support.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupportTicketSummaryResponse {

    private long totalTickets;

    private long openTickets;

    private long inProgressTickets;

    private long waitingCustomerTickets;

    private long resolvedTickets;

    private long closedTickets;
}