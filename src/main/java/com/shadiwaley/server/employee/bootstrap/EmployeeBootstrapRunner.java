package com.shadiwaley.server.employee.bootstrap;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeBootstrapRunner implements CommandLineRunner {

    private final EmployeeAccountRepository employeeAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.super-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.super-admin.email:}")
    private String email;

    @Value("${app.bootstrap.super-admin.password:}")
    private String password;

    @Value("${app.bootstrap.super-admin.full-name:Shadiwaley Super Admin}")
    private String fullName;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Bootstrap super admin email is required");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Bootstrap super admin password is required");
        }

        EmployeeAccount employee = employeeAccountRepository.findByEmailIgnoreCase(email)
                .orElseGet(EmployeeAccount::new);

        employee.setFullName(fullName);
        employee.setEmail(email.toLowerCase());
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setRole(EmployeeRole.SUPER_ADMIN);
        employee.setStatus(EmployeeStatus.ACTIVE);

        employeeAccountRepository.save(employee);
    }
}