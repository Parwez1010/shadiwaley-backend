package com.shadiwaley.server.support.dto.request;

import com.shadiwaley.server.support.domain.SupportTicketCategory;
import com.shadiwaley.server.support.domain.SupportTicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminCreateSupportTicketRequest {

    @NotNull
    private UUID customerUserId;

    @NotBlank
    @Size(max = 180)
    private String subject;

    @NotNull
    private SupportTicketCategory category;

    @NotNull
    private SupportTicketPriority priority;

    @NotBlank
    @Size(max = 3000)
    private String message;

    private UUID assignedEmployeeId;

    @Size(max = 3000)
    private String internalNote;
}