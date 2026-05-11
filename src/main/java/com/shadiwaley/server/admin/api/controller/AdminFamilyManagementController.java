package com.shadiwaley.server.admin.api.controller;

import com.shadiwaley.server.admin.application.service.AdminFamilyService;
import com.shadiwaley.server.admin.dto.request.AssignFamilyCrmRequest;
import com.shadiwaley.server.admin.dto.request.CreateAdminFamilyRequest;
import com.shadiwaley.server.admin.dto.request.UpdateAdminFamilyStatusRequest;
import com.shadiwaley.server.admin.dto.response.CrmFamilyDetailResponse;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/crm/families")
@RequiredArgsConstructor
public class AdminFamilyManagementController {

    private final AdminFamilyService adminFamilyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFamilyDetailResponse> createFamily(
            @Valid @RequestBody CreateAdminFamilyRequest request
    ) {
        return ResponseFactory.success(
                "Family created successfully",
                adminFamilyService.createFamily(request)
        );
    }

    @PatchMapping("/{userId}/onboarding")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFamilyDetailResponse> updateFamilyOnboarding(
            @PathVariable UUID userId,
            @Valid @RequestBody OnboardingProfileUpsertRequest request
    ) {
        return ResponseFactory.success(
                "Family updated successfully",
                adminFamilyService.updateOnboarding(userId, request)
        );
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<CrmFamilyDetailResponse> updateFamilyStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAdminFamilyStatusRequest request
    ) {
        return ResponseFactory.success(
                "Family status updated successfully",
                adminFamilyService.updateStatus(userId, request)
        );
    }

    @PatchMapping("/{userId}/assign-crm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<CrmFamilyDetailResponse> assignCrm(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignFamilyCrmRequest request
    ) {
        return ResponseFactory.success(
                "CRM assigned successfully",
                adminFamilyService.assignCrm(userId, request)
        );
    }
}