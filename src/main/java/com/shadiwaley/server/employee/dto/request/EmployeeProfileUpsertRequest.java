package com.shadiwaley.server.employee.dto.request;

import com.shadiwaley.server.employee.domain.EmployeeGender;
import com.shadiwaley.server.employee.domain.EmployeeWorkMode;
import com.shadiwaley.server.employee.domain.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class EmployeeProfileUpsertRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    private EmployeeGender gender;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String alternatePhone;

    @Email
    @Size(max = 150)
    private String personalEmail;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String district;

    @Size(max = 20)
    private String pincode;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 20)
    private String emergencyContactPhone;

    @Size(max = 50)
    private String employeeCode;

    @Size(max = 120)
    private String designation;

    private EmploymentType employmentType;

    private LocalDate joiningDate;

    private EmployeeWorkMode workMode;

    private UUID reportingManagerId;

    @Size(max = 20)
    private String aadhaarNumber;

    @Size(max = 150)
    private String highestQualification;

    @Size(max = 200)
    private String collegeName;

    private BigDecimal totalExperienceYears;

    @Size(max = 200)
    private String previousCompany;
}