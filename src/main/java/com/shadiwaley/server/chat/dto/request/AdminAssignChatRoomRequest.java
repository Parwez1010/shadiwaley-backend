package com.shadiwaley.server.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminAssignChatRoomRequest {

    @NotNull
    private UUID employeeId;
}