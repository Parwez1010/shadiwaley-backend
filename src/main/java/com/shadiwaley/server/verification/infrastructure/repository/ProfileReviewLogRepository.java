package com.shadiwaley.server.verification.infrastructure.repository;

import com.shadiwaley.server.verification.infrastructure.entity.ProfileReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileReviewLogRepository extends JpaRepository<ProfileReviewLog, UUID> {

    List<ProfileReviewLog> findByUserProfileIdOrderByCreatedAtDesc(UUID userProfileId);
}