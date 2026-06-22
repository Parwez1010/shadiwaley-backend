package com.shadiwaley.server.preferences.infrastructure.repository;

import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    Optional<UserPreferences> findByUserProfileId(UUID userProfileId);

}