package com.shadiwaley.server.proposal.application.service;

import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.AuthUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProposalPermissionService {

    private final EmployeeAccountRepository employeeAccountRepository;

    public EmployeeAccount getCurrentEmployeeOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        String principal = authentication.getName();

        try {
            UUID actorId = AuthUser.getCurrentActorId();
            Optional<EmployeeAccount> byId = employeeAccountRepository.findById(actorId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (Exception ignored) {
        }

        try {
            UUID principalId = UUID.fromString(principal);
            Optional<EmployeeAccount> byPrincipalId = employeeAccountRepository.findById(principalId);
            if (byPrincipalId.isPresent()) {
                return byPrincipalId.get();
            }
        } catch (Exception ignored) {
        }

        if (principal != null && principal.contains("@")) {
            return employeeAccountRepository
                    .findByEmailIgnoreCase(principal.trim().toLowerCase())
                    .orElse(null);
        }

        return null;
    }

    public EmployeeAccount getCurrentEmployeeOrThrow() {
        EmployeeAccount employee = getCurrentEmployeeOrNull();

        if (employee == null) {
            throw new EntityNotFoundException("Logged-in employee not found");
        }

        return employee;
    }

    public void assertCanDispatchForCase(CrmCase crmCase) {
        EmployeeAccount employee = getCurrentEmployeeOrThrow();

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN || employee.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        if (employee.getRole() == EmployeeRole.CRM_AGENT) {
            if (crmCase == null
                    || crmCase.getAssignedEmployee() == null
                    || !crmCase.getAssignedEmployee().getId().equals(employee.getId())) {
                throw new AccessDeniedException("CRM agent can dispatch proposals only for assigned cases");
            }
            return;
        }

        throw new AccessDeniedException("You do not have permission to dispatch proposals");
    }

    public void assertCanAccessCase(CrmCase crmCase) {
        EmployeeAccount employee = getCurrentEmployeeOrThrow();

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN || employee.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        if (employee.getRole() == EmployeeRole.CRM_AGENT) {
            if (crmCase == null
                    || crmCase.getAssignedEmployee() == null
                    || !crmCase.getAssignedEmployee().getId().equals(employee.getId())) {
                throw new AccessDeniedException("CRM agent can access only assigned cases");
            }
            return;
        }

        throw new AccessDeniedException("You do not have permission to access proposals");
    }
}