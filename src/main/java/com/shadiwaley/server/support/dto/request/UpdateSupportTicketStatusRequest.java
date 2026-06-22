package com.shadiwaley.server.support.dto.request;

import com.shadiwaley.server.support.domain.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSupportTicketStatusRequest {

    @NotNull
    private SupportTicketStatus status;
}