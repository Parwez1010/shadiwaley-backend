package com.shadiwaley.server.engagement.infrastructure.repository;

import com.shadiwaley.server.engagement.infrastructure.entity.EngagementLoopState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EngagementLoopStateRepository
        extends JpaRepository<EngagementLoopState, UUID> {

    Optional<EngagementLoopState> findByUserAccountId(UUID userAccountId);
}