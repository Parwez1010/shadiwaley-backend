package com.shadiwaley.server.employee.dto.request;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateEmployeeRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotNull(message = "Role is required")
    private EmployeeRole role;

    @NotBlank(message = "Assigned district is required")
    private String assignedDistrict;

    @NotNull(message = "Base salary is required")
    @Min(value = 0, message = "Base salary cannot be negative")
    private Integer baseSalary;

    @Min(value = 0, message = "Incentive per dispatch cannot be negative")
    private Integer incentivePerDispatch;

    @Min(value = 0, message = "Incentive per engagement cannot be negative")
    private Integer incentivePerEngagement;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @Size(max = 200, message = "Emergency contact cannot exceed 200 characters")
    private String emergencyContact;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}