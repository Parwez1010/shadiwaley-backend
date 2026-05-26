package com.shadiwaley.server.chat.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatNoteResponse {

    private UUID noteId;
    private UUID roomId;
    private String note;
    private UUID createdByEmployeeId;
    private String createdByName;
    private Instant createdAt;
}