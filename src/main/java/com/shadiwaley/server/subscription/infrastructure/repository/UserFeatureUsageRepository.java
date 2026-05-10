package com.shadiwaley.server.subscription.infrastructure.repository;

import com.shadiwaley.server.subscription.infrastructure.entity.UserFeatureUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UserFeatureUsageRepository extends JpaRepository<UserFeatureUsage, UUID> {

    Optional<UserFeatureUsage> findByUserAccountIdAndUsageDate(
            UUID userAccountId,
            LocalDate usageDate
    );

    Optional<UserFeatureUsage> findByUserAccountIdAndUsageMonth(
            UUID userAccountId,
            String usageMonth
    );

}