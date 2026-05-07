package com.shadiwaley.server.profile.infrastructure.repository;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserAccountId(UUID userAccountId);
}