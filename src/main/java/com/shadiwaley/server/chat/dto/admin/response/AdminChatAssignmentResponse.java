package com.shadiwaley.server.chat.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AdminChatAssignmentResponse {

    private UUID roomId;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
}