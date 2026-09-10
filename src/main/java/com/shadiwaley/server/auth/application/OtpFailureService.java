package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.infrastructure.entity.OtpSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpFailureService {

    private final EntityManager entityManager;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(UUID sessionId) {
        // Reload and serialize failure increments in this independent transaction.
        OtpSession session = entityManager.find(OtpSession.class, sessionId, LockModeType.PESSIMISTIC_WRITE);
        if (session == null) {
            throw new IllegalArgumentException("Invalid temp token");
        }
        if (session.isVerified()) {
            throw new IllegalArgumentException("OTP is already verified");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP expired. Please request a new OTP.");
        }
        if (session.getAttemptCount() >= maxAttempts) {
            throw new IllegalArgumentException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        session.setAttemptCount(session.getAttemptCount() + 1);
        // Dirty checking commits before the caller receives this count and throws.
        return session.getAttemptCount();
    }
}
