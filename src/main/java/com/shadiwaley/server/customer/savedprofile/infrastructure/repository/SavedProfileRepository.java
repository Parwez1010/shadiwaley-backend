package com.shadiwaley.server.customer.savedprofile.infrastructure.repository;

import com.shadiwaley.server.customer.savedprofile.entity.SavedProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedProfileRepository extends JpaRepository<SavedProfile, UUID> {

    boolean existsByUserAccountIdAndSavedProfileId(
            UUID userAccountId,
            UUID savedProfileId
    );

    Optional<SavedProfile> findByUserAccountIdAndSavedProfileId(
            UUID userAccountId,
            UUID savedProfileId
    );

    long countByUserAccountId(UUID userAccountId);

    List<SavedProfile> findByUserAccountId(UUID userAccountId);

    List<SavedProfile> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);
}