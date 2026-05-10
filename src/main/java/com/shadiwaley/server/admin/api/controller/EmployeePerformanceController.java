package com.shadiwaley.server.admin.api.controller;

import com.shadiwaley.server.admin.application.service.AdminDashboardService;
import com.shadiwaley.server.admin.dto.response.EmployeePerformanceResponse;
import com.shadiwaley.server.admin.dto.response.TodayFollowUpResponse;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class EmployeePerformanceController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/employees/performance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<EmployeePerformanceResponse>> performance() {

        return ResponseFactory.success(
                "Employee performance fetched successfully",
                adminDashboardService.getEmployeePerformance()
        );
    }

    @GetMapping("/crm/follow-ups/today")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<List<TodayFollowUpResponse>> todayFollowUps() {

        return ResponseFactory.success(
                "Today's follow-ups fetched successfully",
                adminDashboardService.getTodayFollowUps()
        );
    }
}