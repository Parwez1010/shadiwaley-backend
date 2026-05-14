package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.employee.dto.response.EmployeeResponse;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class EmployeeMapper {

    private final CrmCaseRepository crmCaseRepository;

    public EmployeeResponse toResponse(EmployeeAccount employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .role(employee.getRole())
                .status(employee.getStatus())
                .assignedDistrict(employee.getAssignedDistrict())
                .baseSalary(employee.getBaseSalary())
                .incentivePerDispatch(employee.getIncentivePerDispatch())
                .incentivePerEngagement(employee.getIncentivePerEngagement())
                .joiningDate(employee.getJoiningDate())
                .emergencyContact(employee.getEmergencyContact())
                .notes(employee.getNotes())
                .mustChangePassword(employee.isMustChangePassword())
                .passwordChangedAt(employee.getPasswordChangedAt())
                .lastLoginAt(employee.getLastLoginAt())
                .lockedUntil(employee.getLockedUntil())
                .createdAt(employee.getCreatedAt())

                // placeholders until CRM/Rishta/Engagement modules are built
                .familiesAssigned(
                        (int) crmCaseRepository.countByAssignedEmployeeId(employee.getId())
                )
                .dispatchesThisMonth(0)
                .engagementsThisMonth(0)
                .incentiveEarnedThisMonth(employee.getBaseSalary())
                .build();
    }
}