package com.shadiwaley.server.autopilot.infrastructure.repository;

import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AutopilotPreferenceRepository extends JpaRepository<AutopilotPreference, UUID> {
}