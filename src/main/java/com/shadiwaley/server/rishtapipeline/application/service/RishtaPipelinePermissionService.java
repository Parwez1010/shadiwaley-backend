package com.shadiwaley.server.rishtapipeline.application.service;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RishtaPipelinePermissionService {

    private final EmployeeAccountRepository employeeAccountRepository;

    public EmployeeAccount getCurrentEmployee() {
        return employeeAccountRepository.findById(AuthUser.getCurrentActorId())
                .orElseThrow(() -> new AccessDeniedException("Employee not found"));
    }

    public boolean isAdmin(EmployeeAccount employee) {
        return employee.getRole() == EmployeeRole.SUPER_ADMIN
                || employee.getRole() == EmployeeRole.ADMIN;
    }

    public void assertCanViewProposal(Proposal proposal) {
        EmployeeAccount employee = getCurrentEmployee();

        if (isAdmin(employee)) {
            return;
        }

        if (employee.getRole() != EmployeeRole.CRM_AGENT) {
            throw new AccessDeniedException("Access denied");
        }

        if (proposal.getCrmCase() == null
                || proposal.getCrmCase().getAssignedEmployee() == null) {
            throw new AccessDeniedException("Proposal not assigned");
        }

        if (!proposal.getCrmCase().getAssignedEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can only access assigned proposals");
        }
    }

    public void assertCanUpdateProposal(Proposal proposal) {
        assertCanViewProposal(proposal);
    }
}