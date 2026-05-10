package com.shadiwaley.server.crm.dto.response;

import com.shadiwaley.server.crm.domain.CrmNoteType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmNoteResponse {

    private UUID noteId;
    private UUID employeeId;
    private String employeeName;
    private CrmNoteType noteType;
    private String note;
    private Instant createdAt;
}