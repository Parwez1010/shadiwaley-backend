package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatInternalNoteResponse {

    private UUID noteId;

    private UUID roomId;

    private UUID employeeId;

    private String employeeName;

    private String note;

    private Instant createdAt;
}