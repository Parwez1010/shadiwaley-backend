package com.shadiwaley.server.revenue.application.service;

import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevenuePermissionService {

    private final EmployeeAccountRepository employeeAccountRepository;
    private final CrmCaseRepository crmCaseRepository;

    public EmployeeAccount getCurrentEmployeeOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        String principal = authentication.getName();

        try {
            UUID actorId = AuthUser.getCurrentActorId();
            Optional<EmployeeAccount> byId = employeeAccountRepository.findById(actorId);
            if (byId.isPresent()) return byId.get();
        } catch (Exception ignored) {
        }

        try {
            UUID principalId = UUID.fromString(principal);
            Optional<EmployeeAccount> byPrincipalId = employeeAccountRepository.findById(principalId);
            if (byPrincipalId.isPresent()) return byPrincipalId.get();
        } catch (Exception ignored) {
        }

        if (principal != null && principal.contains("@")) {
            return employeeAccountRepository.findByEmailIgnoreCase(principal.trim().toLowerCase()).orElse(null);
        }

        return null;
    }

    public void assertCanManageFamily(UUID userId) {
        EmployeeAccount employee = getCurrentEmployeeOrNull();

        if (employee == null) {
            throw new AccessDeniedException("Employee not found");
        }

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN || employee.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        if (employee.getRole() == EmployeeRole.CRM_AGENT) {
            boolean assigned = crmCaseRepository
                    .findTopByUserAccountIdOrderByUpdatedAtDesc(userId)
                    .map(c -> c.getAssignedEmployee() != null
                            && c.getAssignedEmployee().getId().equals(employee.getId()))
                    .orElse(false);

            if (!assigned) {
                throw new AccessDeniedException("CRM agent can manage payment only for assigned families");
            }

            return;
        }

        throw new AccessDeniedException("You do not have permission to manage revenue");
    }

    public void assertCanViewDashboard() {
        EmployeeAccount employee = getCurrentEmployeeOrNull();

        if (employee == null) {
            throw new AccessDeniedException("Employee not found");
        }

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN || employee.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to view revenue dashboard");
    }

    public void assertCanViewPayments() {
        EmployeeAccount employee = getCurrentEmployeeOrNull();

        if (employee == null) {
            throw new AccessDeniedException("Employee not found");
        }

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN || employee.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to view all payments");
    }


}