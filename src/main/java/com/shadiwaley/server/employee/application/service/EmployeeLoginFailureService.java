package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeLoginFailureService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 30;

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID employeeId) {
        // Reload in this transaction; serialize failure increments for the same employee.
        EmployeeAccount employee = entityManager.find(
                EmployeeAccount.class, employeeId, LockModeType.PESSIMISTIC_WRITE);
        if (employee == null) {
            throw new IllegalArgumentException("Employee account not found");
        }

        int attempts = employee.getFailedLoginAttempts() == null ? 0 : employee.getFailedLoginAttempts();
        attempts++;
        employee.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            employee.setLockedUntil(Instant.now().plusSeconds(LOCK_MINUTES * 60));
        }
        // JPA dirty checking commits both fields before login throws its authentication exception.
    }
}
