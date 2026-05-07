package com.shadiwaley.server.parent.infrastructure.repository;

import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, UUID> {
    Optional<ParentProfile> findByUserAccountId(UUID userAccountId);
}