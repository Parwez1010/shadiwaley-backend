package com.shadiwaley.server.audit.application.service;

import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.audit.dto.response.AuditLogPageResponse;
import com.shadiwaley.server.audit.dto.response.AuditLogResponse;
import com.shadiwaley.server.audit.infrastructure.entity.AuditLog;
import com.shadiwaley.server.audit.infrastructure.repository.AuditLogRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.ActorType;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EmployeeAccountRepository employeeAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final RequestContextService requestContextService;

    @Transactional
    public void record(
            AuditAction action,
            AuditEntityType entityType,
            UUID entityId,
            String description
    ) {
        record(action, entityType, entityId, description, null);
    }

    @Transactional
    public void record(
            AuditAction action,
            AuditEntityType entityType,
            UUID entityId,
            String description,
            String metadata
    ) {
        AuditLog log = new AuditLog();

        fillActor(log);

        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setMetadata(metadata);
        log.setIpAddress(requestContextService.getIpAddress());
        log.setUserAgent(requestContextService.getUserAgent());

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse getLogs(int page, int size, AuditAction action) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLog> result = action == null
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);

        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse getEntityLogs(
            AuditEntityType entityType,
            UUID entityId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLog> result =
                auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType,
                        entityId,
                        pageable
                );

        return toPageResponse(result);
    }

    private void fillActor(AuditLog log) {
        try {
            UUID actorId = AuthUser.getCurrentActorId();
            ActorType actorType = AuthUser.getCurrentActorType();

            log.setActorId(actorId);
            log.setActorType(actorType.name());

            if (actorType == ActorType.EMPLOYEE) {
                EmployeeAccount employee = employeeAccountRepository.findById(actorId).orElse(null);

                if (employee != null) {
                    log.setActorName(employee.getFullName());
                    log.setActorRole(employee.getRole().name());
                }
            } else {
                UserAccount user = userAccountRepository.findById(actorId).orElse(null);

                if (user != null) {
                    log.setActorName(user.getPhone());
                    log.setActorRole(user.getRole().name());
                }
            }

        } catch (Exception ignored) {
            log.setActorType("SYSTEM");
            log.setActorName("System");
            log.setActorRole("SYSTEM");
        }
    }

    private AuditLogPageResponse toPageResponse(Page<AuditLog> page) {
        return AuditLogPageResponse.builder()
                .logs(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .auditLogId(log.getId())
                .actorId(log.getActorId())
                .actorType(log.getActorType())
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}