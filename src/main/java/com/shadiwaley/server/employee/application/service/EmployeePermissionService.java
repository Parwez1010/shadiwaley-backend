package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeePermissionService {

    public List<String> getPermissions(EmployeeRole role) {
        return switch (role) {
            case SUPER_ADMIN -> List.of(
                    "DASHBOARD_VIEW",
                    "USER_VIEW",
                    "USER_EDIT",
                    "USER_DELETE",
                    "PROFILE_VERIFY",
                    "MEDIA_VERIFY",
                    "EMPLOYEE_VIEW",
                    "EMPLOYEE_CREATE",
                    "EMPLOYEE_EDIT",
                    "EMPLOYEE_DELETE",
                    "REVENUE_VIEW",
                    "REVENUE_MANAGE",
                    "AUDIT_LOG_VIEW",
                    "SUPPORT_VIEW",
                    "SUPPORT_MANAGE"
            );

            case ADMIN -> List.of(
                    "DASHBOARD_VIEW",
                    "USER_VIEW",
                    "USER_EDIT",
                    "PROFILE_VERIFY",
                    "MEDIA_VERIFY",
                    "EMPLOYEE_VIEW",
                    "REVENUE_VIEW",
                    "AUDIT_LOG_VIEW",
                    "SUPPORT_VIEW"
            );

            case CRM_AGENT -> List.of(
                    "CRM_DASHBOARD_VIEW",
                    "USER_VIEW_DISTRICT",
                    "RISHTA_MANAGE_ASSIGNED",
                    "CHAT_MODERATE_ASSIGNED",
                    "AUTOPILOT_MANAGE_ASSIGNED",
                    "SUPPORT_MANAGE_ASSIGNED"
            );

            case VERIFIER -> List.of(
                    "VERIFICATION_QUEUE_VIEW",
                    "PROFILE_VERIFY",
                    "MEDIA_VERIFY"
            );

            case SUPPORT_AGENT -> List.of(
                    "SUPPORT_VIEW",
                    "SUPPORT_MANAGE"
            );
        };
    }
}