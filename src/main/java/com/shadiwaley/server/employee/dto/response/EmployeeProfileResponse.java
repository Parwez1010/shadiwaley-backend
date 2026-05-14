package com.shadiwaley.server.employee.dto.response;

import com.shadiwaley.server.employee.domain.EmployeeGender;
import com.shadiwaley.server.employee.domain.EmployeeWorkMode;
import com.shadiwaley.server.employee.domain.EmploymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class EmployeeProfileResponse {

    private UUID employeeId;

    private String firstName;
    private String lastName;
    private EmployeeGender gender;
    private LocalDate dateOfBirth;

    private String alternatePhone;
    private String personalEmail;

    private String address;
    private String city;
    private String state;
    private String district;
    private String pincode;

    private String emergencyContactName;
    private String emergencyContactPhone;

    private String employeeCode;
    private String designation;
    private EmploymentType employmentType;
    private LocalDate joiningDate;
    private EmployeeWorkMode workMode;

    private UUID reportingManagerId;
    private String reportingManagerName;

    private String aadhaarMasked;

    private String highestQualification;
    private String collegeName;
    private BigDecimal totalExperienceYears;
    private String previousCompany;

    private List<EmployeeDocumentResponse> documents;
}