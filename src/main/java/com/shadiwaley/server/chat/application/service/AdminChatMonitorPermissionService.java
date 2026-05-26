package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatRoom;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminChatMonitorPermissionService {

    private final EmployeeAccountRepository employeeAccountRepository;

    public EmployeeAccount getCurrentEmployee() {
        return employeeAccountRepository.findById(AuthUser.getCurrentActorId())
                .orElseThrow(() -> new AccessDeniedException("Employee account not found"));
    }

    public boolean isAdmin(EmployeeAccount employee) {
        return employee.getRole() == EmployeeRole.SUPER_ADMIN
                || employee.getRole() == EmployeeRole.ADMIN;
    }

    public void assertCanViewRoom(FamilyChatRoom room) {
        EmployeeAccount employee = getCurrentEmployee();

        if (isAdmin(employee)) {
            return;
        }

        if (employee.getRole() != EmployeeRole.CRM_AGENT) {
            throw new AccessDeniedException("You are not allowed to access chat monitor");
        }

        if (room.getAssignedEmployee() == null
                || !room.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can access only assigned chat rooms");
        }
    }

    public void assertCanUpdateRoom(FamilyChatRoom room) {
        assertCanViewRoom(room);
    }

    public void assertCanAssignRoom() {
        EmployeeAccount employee = getCurrentEmployee();

        if (!isAdmin(employee)) {
            throw new AccessDeniedException("Only admin can assign chat rooms");
        }
    }

    public void assertCanModerateMessage(FamilyChatRoom room) {
        EmployeeAccount employee = getCurrentEmployee();

        if (isAdmin(employee)) {
            return;
        }

        if (employee.getRole() == EmployeeRole.CRM_AGENT
                && room.getAssignedEmployee() != null
                && room.getAssignedEmployee().getId().equals(employee.getId())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to moderate this message");
    }
}