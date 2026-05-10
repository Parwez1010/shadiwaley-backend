package com.shadiwaley.server.admin.api.controller;

import com.shadiwaley.server.admin.application.service.AdminDashboardService;
import com.shadiwaley.server.admin.dto.response.AdminDashboardSummaryResponse;
import com.shadiwaley.server.admin.dto.response.PendingActionResponse;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT', 'VERIFIER')")
    public ApiResponse<AdminDashboardSummaryResponse> summary() {
        return ResponseFactory.success(
                "Dashboard summary fetched successfully",
                adminDashboardService.getSummary()
        );
    }

    @GetMapping("/pending-actions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT', 'VERIFIER')")
    public ApiResponse<List<PendingActionResponse>> pendingActions() {
        return ResponseFactory.success(
                "Pending actions fetched successfully",
                adminDashboardService.getPendingActions()
        );
    }
}