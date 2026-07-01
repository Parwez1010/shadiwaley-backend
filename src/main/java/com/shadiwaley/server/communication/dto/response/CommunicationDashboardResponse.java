package com.shadiwaley.server.communication.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunicationDashboardResponse {

    private long totalOpenItems;

    private long myAssignedItems;

    private long unassignedItems;

    private long needsAttentionItems;

    private long reportedChats;

    private long openSupportTickets;

    private long waitingCustomerTickets;

    private long activeChatRooms;

    private long crmAssistedChats;

    private long premiumFamilies;

    private long eliteFamilies;

    private long messagesToday;

    private long supportRepliesToday;
}