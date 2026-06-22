package com.shadiwaley.server.support.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignSupportTicketRequest {

    @NotNull
    private UUID employeeId;
}