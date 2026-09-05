package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.dto.request.AdminLoginRequest;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeDocumentRepository;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeProfileRepository;
import com.shadiwaley.server.security.JwtService;
import com.shadiwaley.server.security.PasswordConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = AdminAuthFailurePersistenceTest.PersistenceConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminAuthFailurePersistenceTest {
    @TestConfiguration
    @EnableJpaRepositories(basePackageClasses = EmployeeAccountRepository.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {EmployeeProfileRepository.class, EmployeeDocumentRepository.class}))
    @Import({AdminAuthService.class, EmployeeLoginFailureService.class, PasswordConfig.class})
    static class PersistenceConfig {
        @Bean
        PersistenceManagedTypes persistenceManagedTypes() {
            return PersistenceManagedTypes.of(EmployeeAccount.class.getName());
        }
    }

    @Autowired AdminAuthService authService;
    @Autowired EmployeeAccountRepository employees;
    @Autowired PasswordEncoder encoder;
    @MockBean JwtService jwtService;
    @MockBean EmployeePermissionService permissionService;
    private UUID employeeId;

    @BeforeEach
    void createEmployee() {
        employees.deleteAll();
        EmployeeAccount employee = new EmployeeAccount();
        employee.setFullName("Login regression employee");
        employee.setEmail("login-test@example.com");
        employee.setPasswordHash(encoder.encode("CorrectPassword1"));
        employee.setRole(EmployeeRole.ADMIN);
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeId = employees.saveAndFlush(employee).getId();
    }

    @Test
    void wrongPasswordCommitsAttemptDespiteAuthenticationException() {
        rejectWrongPassword();
        EmployeeAccount persisted = readEmployee();
        assertThat(persisted.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLastLoginAt()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void repeatedFailuresPersistAndLockAtFiveAttempts() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            Instant before = Instant.now();
            rejectWrongPassword();
            EmployeeAccount persisted = readEmployee();
            assertThat(persisted.getFailedLoginAttempts()).isEqualTo(attempt);
            if (attempt < 5) {
                assertThat(persisted.getLockedUntil()).isNull();
            } else {
                assertThirtyMinuteLock(persisted, before);
            }
        }
        Instant lockedUntil = readEmployee().getLockedUntil();
        assertThatThrownBy(() -> authService.login(request("CorrectPassword1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Too many failed login attempts. Try again after ");
        assertThat(readEmployee().getFailedLoginAttempts()).isEqualTo(5);
        assertThat(readEmployee().getLockedUntil()).isEqualTo(lockedUntil);
        verifyNoInteractions(jwtService);
    }

    @Test
    void thresholdFailureCommitsLockoutDespiteAuthenticationException() {
        EmployeeAccount employee = readEmployee();
        employee.setFailedLoginAttempts(4);
        employees.saveAndFlush(employee);
        Instant before = Instant.now();
        rejectWrongPassword();
        EmployeeAccount persisted = readEmployee();
        assertThat(persisted.getFailedLoginAttempts()).isEqualTo(5);
        assertThirtyMinuteLock(persisted, before);
    }

    @Test
    void successfulLoginStillResetsFailureState() {
        seedExpiredLock();
        when(jwtService.generateEmployeeAccessToken(any())).thenReturn("access-token");
        assertThat(authService.login(request("CorrectPassword1")).getAccessToken()).isEqualTo("access-token");
        EmployeeAccount persisted = readEmployee();
        assertThat(persisted.getFailedLoginAttempts()).isZero();
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLastLoginAt()).isNotNull();
    }

    @Test
    void unrelatedFailureStillRollsBackSuccessfulLoginWrites() {
        Instant previousLock = seedExpiredLock();
        when(jwtService.generateEmployeeAccessToken(any())).thenThrow(new IllegalStateException("Token generation failed"));
        assertThatThrownBy(() -> authService.login(request("CorrectPassword1")))
                .isInstanceOf(IllegalStateException.class).hasMessage("Token generation failed");
        EmployeeAccount persisted = readEmployee();
        assertThat(persisted.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(persisted.getLockedUntil()).isEqualTo(previousLock);
        assertThat(persisted.getLastLoginAt()).isNull();
    }

    private void rejectWrongPassword() {
        assertThatThrownBy(() -> authService.login(request("WrongPassword1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid email or password");
    }

    private EmployeeAccount readEmployee() {
        // Each repository call reads committed state after the login transaction has exited.
        return employees.findById(employeeId).orElseThrow();
    }

    private Instant seedExpiredLock() {
        EmployeeAccount employee = readEmployee();
        Instant expired = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        employee.setFailedLoginAttempts(5);
        employee.setLockedUntil(expired);
        employees.saveAndFlush(employee);
        return expired;
    }

    private void assertThirtyMinuteLock(EmployeeAccount employee, Instant before) {
        assertThat(employee.getLockedUntil()).isBetween(
                before.plusSeconds(1800).minusMillis(1), Instant.now().plusSeconds(1800).plusMillis(1));
    }

    private AdminLoginRequest request(String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("login-test@example.com");
        request.setPassword(password);
        return request;
    }
}
