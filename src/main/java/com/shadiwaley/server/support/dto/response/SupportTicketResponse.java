package com.shadiwaley.server.support.dto.response;

import com.shadiwaley.server.support.domain.SupportTicketCategory;
import com.shadiwaley.server.support.domain.SupportTicketPriority;
import com.shadiwaley.server.support.domain.SupportTicketSource;
import com.shadiwaley.server.support.domain.SupportTicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SupportTicketResponse {

    private UUID ticketId;

    private String subject;

    private SupportTicketCategory category;

    private SupportTicketPriority priority;

    private SupportTicketStatus status;

    private String lastMessage;

    private UUID assignedEmployeeId;

    private String assignedEmployeeName;

    private SupportTicketSource source;
    private UUID customerUserId;
    private String customerName;
    private String customerPhone;


    private Instant lastRepliedAt;

    private Instant createdAt;

    private Instant updatedAt;
}