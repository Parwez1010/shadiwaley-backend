package com.shadiwaley.server.employee.infrastructure.entity;

import com.shadiwaley.server.employee.domain.EmployeeGender;
import com.shadiwaley.server.employee.domain.EmployeeWorkMode;
import com.shadiwaley.server.employee.domain.EmploymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee_profile")
public class EmployeeProfile {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_account_id", nullable = false, unique = true)
    private EmployeeAccount employeeAccount;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmployeeGender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(name = "personal_email", length = 150)
    private String personalEmail;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String district;

    @Column(length = 20)
    private String pincode;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "employee_code", unique = true, length = 50)
    private String employeeCode;

    @Column(length = 120)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 50)
    private EmploymentType employmentType;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 50)
    private EmployeeWorkMode workMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private EmployeeAccount reportingManager;

    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    @Column(name = "highest_qualification", length = 150)
    private String highestQualification;

    @Column(name = "college_name", length = 200)
    private String collegeName;

    @Column(name = "total_experience_years", precision = 4, scale = 1)
    private BigDecimal totalExperienceYears;

    @Column(name = "previous_company", length = 200)
    private String previousCompany;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}