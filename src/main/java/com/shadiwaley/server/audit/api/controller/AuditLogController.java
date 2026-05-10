package com.shadiwaley.server.audit.api.controller;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.audit.dto.response.AuditLogPageResponse;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<AuditLogPageResponse> logs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AuditAction action
    ) {
        return ResponseFactory.success(
                "Audit logs fetched successfully",
                auditLogService.getLogs(page, size, action)
        );
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<AuditLogPageResponse> entityLogs(
            @PathVariable AuditEntityType entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Entity audit logs fetched successfully",
                auditLogService.getEntityLogs(entityType, entityId, page, size)
        );
    }
}