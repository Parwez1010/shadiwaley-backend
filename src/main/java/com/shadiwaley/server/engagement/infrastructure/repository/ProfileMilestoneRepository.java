package com.shadiwaley.server.engagement.infrastructure.repository;

import com.shadiwaley.server.engagement.domain.MilestoneCode;
import com.shadiwaley.server.engagement.infrastructure.entity.ProfileMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileMilestoneRepository
        extends JpaRepository<ProfileMilestone, UUID> {

    List<ProfileMilestone> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);

    Optional<ProfileMilestone> findByUserAccountIdAndMilestoneCode(
            UUID userAccountId,
            MilestoneCode milestoneCode
    );
}