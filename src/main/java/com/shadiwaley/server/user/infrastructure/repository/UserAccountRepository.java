package com.shadiwaley.server.user.infrastructure.repository;

import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByPhone(String phone);
    boolean existsByPhone(String phone);
    long countByAccountStatus(String accountStatus);
}