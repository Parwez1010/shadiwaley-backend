package com.shadiwaley.server.support.dto.request;

import com.shadiwaley.server.support.domain.SupportTicketPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSupportTicketPriorityRequest {

    @NotNull
    private SupportTicketPriority priority;
}