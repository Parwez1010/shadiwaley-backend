package com.shadiwaley.server.auth.infrastructure.repository;

import com.shadiwaley.server.auth.infrastructure.entity.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {

    Optional<OtpSession> findByTempToken(UUID tempToken);

    Optional<OtpSession> findTopByPhoneOrderByCreatedAtDesc(String phone);

    long countByPhoneAndCreatedAtAfter(String phone, Instant after);
}