package com.shadiwaley.server.chat.dto.admin.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminChatAssignmentRequest {

    @NotNull
    private UUID assignedEmployeeId;

    private String note;
}