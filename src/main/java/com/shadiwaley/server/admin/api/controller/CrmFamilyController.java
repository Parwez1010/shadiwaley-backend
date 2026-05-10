package com.shadiwaley.server.admin.api.controller;

import com.shadiwaley.server.admin.application.service.AdminDashboardService;
import com.shadiwaley.server.admin.dto.response.CrmFamilyDetailResponse;
import com.shadiwaley.server.admin.dto.response.CrmFamilyListResponse;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/crm/families")
@RequiredArgsConstructor
public class CrmFamilyController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<Page<CrmFamilyListResponse>> families(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) ProfileStatus status,
            @RequestParam(required = false) UserSide side,
            @RequestParam(required = false) String search
    ) {

        return ResponseFactory.success(
                "Families fetched successfully",
                adminDashboardService.getFamilies(
                        page,
                        size,
                        district,
                        status,
                        side,
                        search
                )
        );
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFamilyDetailResponse> familyDetail(
            @PathVariable UUID userId
    ) {

        return ResponseFactory.success(
                "Family detail fetched successfully",
                adminDashboardService.getFamilyDetail(userId)
        );
    }
}