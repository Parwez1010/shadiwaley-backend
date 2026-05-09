package com.shadiwaley.server.employee.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.employee.application.service.EmployeeService;
import com.shadiwaley.server.employee.dto.request.CreateEmployeeRequest;
import com.shadiwaley.server.employee.dto.request.UpdateEmployeeRequest;
import com.shadiwaley.server.employee.dto.request.UpdateEmployeeStatusRequest;
import com.shadiwaley.server.employee.dto.response.CreateEmployeeResponse;
import com.shadiwaley.server.employee.dto.response.EmployeeResponse;
import com.shadiwaley.server.employee.dto.response.ResetPasswordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<CreateEmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        return ResponseFactory.success(
                "Employee created successfully",
                employeeService.createEmployee(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<EmployeeResponse>> getEmployees() {
        return ResponseFactory.success(
                "Employees fetched successfully",
                employeeService.getEmployees()
        );
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<EmployeeResponse> updateEmployee(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseFactory.success(
                "Employee updated successfully",
                employeeService.updateEmployee(employeeId, request)
        );
    }

    @PatchMapping("/{employeeId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<EmployeeResponse> updateStatus(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeStatusRequest request
    ) {
        return ResponseFactory.success(
                "Employee status updated successfully",
                employeeService.updateStatus(employeeId, request)
        );
    }

    @PostMapping("/{employeeId}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<ResetPasswordResponse> resetPassword(
            @PathVariable UUID employeeId
    ) {
        return ResponseFactory.success(
                "Temporary password generated successfully",
                employeeService.resetPassword(employeeId)
        );
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deactivateEmployee(
            @PathVariable UUID employeeId
    ) {
        employeeService.deactivateEmployee(employeeId);

        return ResponseFactory.success(
                "Employee deactivated successfully",
                null
        );
    }
}