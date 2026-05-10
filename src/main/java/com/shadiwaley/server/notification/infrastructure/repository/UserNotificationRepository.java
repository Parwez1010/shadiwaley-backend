package com.shadiwaley.server.notification.infrastructure.repository;

import com.shadiwaley.server.notification.infrastructure.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);

    Optional<UserNotification> findByIdAndUserAccountId(UUID id, UUID userAccountId);

    long countByUserAccountIdAndReadFalse(UUID userAccountId);

    List<UserNotification> findByUserAccountIdAndReadFalse(UUID userAccountId);
}