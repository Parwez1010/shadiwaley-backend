package com.shadiwaley.server.subscription.infrastructure.repository;

import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.subscription.infrastructure.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByUserAccountIdAndStatusOrderByCreatedAtDesc(
            UUID userAccountId,
            SubscriptionStatus status
    );
}