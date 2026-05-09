package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.dto.request.CreateEmployeeRequest;
import com.shadiwaley.server.employee.dto.request.UpdateEmployeeRequest;
import com.shadiwaley.server.employee.dto.request.UpdateEmployeeStatusRequest;
import com.shadiwaley.server.employee.dto.response.CreateEmployeeResponse;
import com.shadiwaley.server.employee.dto.response.EmployeeResponse;
import com.shadiwaley.server.employee.dto.response.ResetPasswordResponse;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$";

    private final EmployeeAccountRepository employeeAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public CreateEmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeAccountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("An employee with this email already exists");
        }

        if (request.getRole() == EmployeeRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin cannot be created from this API");
        }

        String temporaryPassword = generateTemporaryPassword();

        EmployeeAccount employee = new EmployeeAccount();
        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail().toLowerCase().trim());
        employee.setPhone(request.getPhone());
        employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        employee.setMustChangePassword(true);
        employee.setRole(request.getRole());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setAssignedDistrict(request.getAssignedDistrict());
        employee.setBaseSalary(request.getBaseSalary());
        employee.setIncentivePerDispatch(defaultIfNull(request.getIncentivePerDispatch(), 500));
        employee.setIncentivePerEngagement(defaultIfNull(request.getIncentivePerEngagement(), 2000));
        employee.setJoiningDate(request.getJoiningDate());
        employee.setEmergencyContact(request.getEmergencyContact());
        employee.setNotes(request.getNotes());

        /*
         * Email integration will be added later.
         * For now, Super Admin can reset password and share temporary password manually.
         */
        EmployeeAccount saved = employeeAccountRepository.save(employee);

        return CreateEmployeeResponse.builder()
                .employeeId(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .temporaryPassword(temporaryPassword)
                .mustChangePassword(true)
                .build();
    }

    public List<EmployeeResponse> getEmployees() {
        return employeeAccountRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID employeeId, UpdateEmployeeRequest request) {
        EmployeeAccount employee = getEmployee(employeeId);

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin cannot be modified from this API");
        }

        employeeAccountRepository.findByEmailIgnoreCase(request.getEmail())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(employeeId)) {
                        throw new IllegalArgumentException("Another employee already uses this email");
                    }
                });

        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail().toLowerCase().trim());
        employee.setPhone(request.getPhone());
        employee.setRole(request.getRole());
        employee.setStatus(request.getStatus());
        employee.setAssignedDistrict(request.getAssignedDistrict());
        employee.setBaseSalary(request.getBaseSalary());
        employee.setIncentivePerDispatch(defaultIfNull(request.getIncentivePerDispatch(), 500));
        employee.setIncentivePerEngagement(defaultIfNull(request.getIncentivePerEngagement(), 2000));
        employee.setJoiningDate(request.getJoiningDate());
        employee.setEmergencyContact(request.getEmergencyContact());
        employee.setNotes(request.getNotes());

        return employeeMapper.toResponse(employeeAccountRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateStatus(UUID employeeId, UpdateEmployeeStatusRequest request) {
        EmployeeAccount employee = getEmployee(employeeId);

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin status cannot be changed from this API");
        }

        employee.setStatus(request.getStatus());

        return employeeMapper.toResponse(employeeAccountRepository.save(employee));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(UUID employeeId) {
        EmployeeAccount employee = getEmployee(employeeId);

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin password cannot be reset from this API");
        }

        String temporaryPassword = generateTemporaryPassword();

        employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        employee.setMustChangePassword(true);
        employee.setPasswordChangedAt(null);
        employee.setFailedLoginAttempts(0);
        employee.setLockedUntil(null);

        employeeAccountRepository.save(employee);

        return ResetPasswordResponse.builder()
                .employeeId(employee.getId())
                .email(employee.getEmail())
                .temporaryPassword(temporaryPassword)
                .mustChangePassword(true)
                .build();
    }

    @Transactional
    public void deactivateEmployee(UUID employeeId) {
        EmployeeAccount employee = getEmployee(employeeId);

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin cannot be deleted from this API");
        }

        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setDeletedAt(Instant.now());

        employeeAccountRepository.save(employee);
    }

    private EmployeeAccount getEmployee(UUID employeeId) {
        EmployeeAccount employee = employeeAccountRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (employee.getDeletedAt() != null) {
            throw new IllegalArgumentException("Employee has already been deactivated");
        }

        return employee;
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(
                    SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())
            ));
        }

        password.append("1a");

        return password.toString();
    }
}