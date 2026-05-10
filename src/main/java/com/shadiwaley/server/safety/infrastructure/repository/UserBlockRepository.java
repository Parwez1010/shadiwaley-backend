package com.shadiwaley.server.safety.infrastructure.repository;

import com.shadiwaley.server.safety.infrastructure.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerUserIdAndBlockedUserId(UUID blockerUserId, UUID blockedUserId);

    Optional<UserBlock> findByBlockerUserIdAndBlockedUserId(UUID blockerUserId, UUID blockedUserId);
}