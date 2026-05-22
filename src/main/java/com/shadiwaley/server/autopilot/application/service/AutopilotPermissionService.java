package com.shadiwaley.server.autopilot.application.service;

import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchQueue;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutopilotPermissionService {

    private final EmployeeAccountRepository employeeAccountRepository;

    public EmployeeAccount getCurrentEmployee() {
        return employeeAccountRepository.findById(AuthUser.getCurrentActorId())
                .orElseThrow(() -> new AccessDeniedException("Employee not found"));
    }

    public boolean isAdmin(EmployeeAccount employee) {
        return employee.getRole() == EmployeeRole.SUPER_ADMIN
                || employee.getRole() == EmployeeRole.ADMIN;
    }

    public void assertCanViewQueue(AutopilotDispatchQueue queue) {
        EmployeeAccount employee = getCurrentEmployee();

        if (isAdmin(employee)) {
            return;
        }

        if (employee.getRole() != EmployeeRole.CRM_AGENT) {
            throw new AccessDeniedException("Access denied");
        }

        if (queue.getAssignedEmployee() == null
                || !queue.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can only access assigned dispatch queue");
        }
    }

    public UUID effectiveAssignedEmployeeId(UUID requestedEmployeeId) {
        EmployeeAccount employee = getCurrentEmployee();

        if (isAdmin(employee)) {
            return requestedEmployeeId;
        }

        if (employee.getRole() == EmployeeRole.CRM_AGENT) {
            return employee.getId();
        }

        throw new AccessDeniedException("Access denied");
    }
}