package com.shadiwaley.server.audit.infrastructure.repository;

import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.audit.infrastructure.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            AuditEntityType entityType,
            UUID entityId,
            Pageable pageable
    );

    Page<AuditLog> findByActionOrderByCreatedAtDesc(
            AuditAction action,
            Pageable pageable
    );

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}