package com.shadiwaley.server.audit.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AuditLogPageResponse {

    private List<AuditLogResponse> logs;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}