package com.shadiwaley.server.employee.infrastructure.entity;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee_account")
public class EmployeeAccount {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmployeeRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployeeStatus status;

    @Column(name = "assigned_district", length = 100)
    private String assignedDistrict;

    @Column(name = "base_salary", nullable = false)
    private Integer baseSalary;

    @Column(name = "incentive_per_dispatch", nullable = false)
    private Integer incentivePerDispatch;

    @Column(name = "incentive_per_engagement", nullable = false)
    private Integer incentivePerEngagement;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "emergency_contact", length = 200)
    private String emergencyContact;

    @Column(length = 500)
    private String notes;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (status == null) status = EmployeeStatus.ACTIVE;
        if (baseSalary == null) baseSalary = 0;
        if (incentivePerDispatch == null) incentivePerDispatch = 500;
        if (incentivePerEngagement == null) incentivePerEngagement = 2000;
        if (failedLoginAttempts == null) failedLoginAttempts = 0;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}