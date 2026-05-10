package com.shadiwaley.server.profile.infrastructure.repository;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID>, JpaSpecificationExecutor<UserProfile> {
    Optional<UserProfile> findByUserAccountId(UUID userAccountId);
    long countByProfileStatus(com.shadiwaley.server.profile.domain.ProfileStatus profileStatus);

    java.util.List<com.shadiwaley.server.profile.infrastructure.entity.UserProfile>
    findTop20ByProfileStatusOrderByCreatedAtDesc(
            com.shadiwaley.server.profile.domain.ProfileStatus profileStatus
    );
}