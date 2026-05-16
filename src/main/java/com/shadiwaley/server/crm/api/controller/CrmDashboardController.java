package com.shadiwaley.server.crm.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.crm.application.service.CrmDashboardService;
import com.shadiwaley.server.crm.dto.response.CrmDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/crm")
@RequiredArgsConstructor
public class CrmDashboardController {

    private final CrmDashboardService crmDashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmDashboardResponse> getDashboard() {
        return ResponseFactory.success(
                "CRM dashboard fetched successfully",
                crmDashboardService.getDashboard()
        );
    }
}