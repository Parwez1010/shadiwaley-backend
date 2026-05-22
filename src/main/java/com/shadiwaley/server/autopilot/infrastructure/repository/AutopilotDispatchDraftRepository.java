package com.shadiwaley.server.autopilot.infrastructure.repository;

import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AutopilotDispatchDraftRepository
        extends JpaRepository<AutopilotDispatchDraft, UUID> {
}