package com.shadiwaley.server.audit.dto.response;

import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {

    private UUID auditLogId;

    private UUID actorId;
    private String actorType;
    private String actorName;
    private String actorRole;

    private AuditAction action;

    private AuditEntityType entityType;
    private UUID entityId;

    private String description;
    private String metadata;

    private String ipAddress;
    private String userAgent;

    private Instant createdAt;
}