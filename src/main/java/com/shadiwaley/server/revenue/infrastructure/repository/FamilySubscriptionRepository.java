package com.shadiwaley.server.revenue.infrastructure.repository;

import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.revenue.domain.SubscriptionStatus;
import com.shadiwaley.server.revenue.infrastructure.entity.FamilySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FamilySubscriptionRepository extends JpaRepository<FamilySubscription, UUID> {

    Optional<FamilySubscription> findTopByUserAccountIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(UUID userId);

    Optional<FamilySubscription> findTopByUserProfileIdAndCurrentSubscriptionTrueOrderByCreatedAtDesc(UUID profileId);

    long countBySubscriptionStatus(SubscriptionStatus status);

    long countByPaymentStatus(RevenuePaymentStatus status);
}