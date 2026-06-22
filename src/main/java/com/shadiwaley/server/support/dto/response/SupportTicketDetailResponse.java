package com.shadiwaley.server.support.dto.response;

import com.shadiwaley.server.support.domain.SupportTicketCategory;
import com.shadiwaley.server.support.domain.SupportTicketPriority;
import com.shadiwaley.server.support.domain.SupportTicketSource;
import com.shadiwaley.server.support.domain.SupportTicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SupportTicketDetailResponse {

    private UUID ticketId;

    private String subject;

    private SupportTicketCategory category;

    private SupportTicketPriority priority;

    private SupportTicketStatus status;

    private UUID customerUserId;

    private String customerName;

    private String customerPhone;

    private UUID assignedEmployeeId;

    private String assignedEmployeeName;

    private SupportTicketSource source;
    private UUID createdByEmployeeId;
    private String createdByEmployeeName;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant resolvedAt;

    private Instant closedAt;

    private List<SupportTicketReplyResponse> replies;
}