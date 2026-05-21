package com.shadiwaley.server.rishtapipeline.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RishtaPipelineNoteResponse {

    private UUID noteId;
    private UUID proposalId;
    private String note;
    private String createdByName;
    private Instant createdAt;
}