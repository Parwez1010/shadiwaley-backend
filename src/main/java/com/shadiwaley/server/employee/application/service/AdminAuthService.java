package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.dto.request.AdminLoginRequest;
import com.shadiwaley.server.employee.dto.request.ChangePasswordRequest;
import com.shadiwaley.server.employee.dto.response.AdminLoginResponse;
import com.shadiwaley.server.employee.dto.response.AdminMeResponse;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final EmployeeAccountRepository employeeAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmployeePermissionService permissionService;
    private final EmployeeLoginFailureService loginFailureService;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        EmployeeAccount employee = employeeAccountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (employee.getDeletedAt() != null) {
            throw new IllegalArgumentException("This employee account is no longer active.");
        }

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Your employee account is not active. Please contact the administrator.");
        }

        if (employee.getLockedUntil() != null && employee.getLockedUntil().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Too many failed login attempts. Try again after " + employee.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            loginFailureService.recordFailure(employee.getId());
            throw new IllegalArgumentException("Invalid email or password");
        }

        employee.setFailedLoginAttempts(0);
        employee.setLockedUntil(null);
        employee.setLastLoginAt(Instant.now());
        employeeAccountRepository.save(employee);

        return AdminLoginResponse.builder()
                .accessToken(jwtService.generateEmployeeAccessToken(employee))
                .employeeId(employee.getId())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .assignedDistrict(employee.getAssignedDistrict())
                .role(employee.getRole())
                .mustChangePassword(employee.isMustChangePassword())
                .permissions(permissionService.getPermissions(employee.getRole()))
                .build();
    }

    public AdminMeResponse me() {
        EmployeeAccount employee = getCurrentEmployee();

        return AdminMeResponse.builder()
                .employeeId(employee.getId())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .role(employee.getRole())
                .status(employee.getStatus())
                .assignedDistrict(employee.getAssignedDistrict())
                .mustChangePassword(employee.isMustChangePassword())
                .permissions(permissionService.getPermissions(employee.getRole()))
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        EmployeeAccount employee = getCurrentEmployee();

        if (!passwordEncoder.matches(request.getCurrentPassword(), employee.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), employee.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        employee.setMustChangePassword(false);
        employee.setPasswordChangedAt(Instant.now());

        employeeAccountRepository.save(employee);
    }

    private EmployeeAccount getCurrentEmployee() {
        return employeeAccountRepository.findById(AuthUser.getCurrentActorId())
                .orElseThrow(() -> new IllegalArgumentException("Employee account not found"));
    }
}
