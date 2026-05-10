package com.shadiwaley.server.crm.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.crm.application.service.CrmCaseService;
import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.dto.request.*;
import com.shadiwaley.server.crm.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/crm/cases")
@RequiredArgsConstructor
public class CrmCaseController {

    private final CrmCaseService crmCaseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmCaseResponse> createCase(@Valid @RequestBody CreateCrmCaseRequest request) {
        return ResponseFactory.success(
                "CRM case created successfully",
                crmCaseService.createCase(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmCasePageResponse> getCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CrmCaseStatus status,
            @RequestParam(required = false) CrmCasePriority priority,
            @RequestParam(required = false) UUID assignedEmployeeId,
            @RequestParam(required = false) String search
    ) {
        return ResponseFactory.success(
                "CRM cases fetched successfully",
                crmCaseService.getCases(page, size, status, priority, assignedEmployeeId, search)
        );
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmCaseDetailResponse> getCaseDetail(@PathVariable UUID caseId) {
        return ResponseFactory.success(
                "CRM case detail fetched successfully",
                crmCaseService.getCaseDetail(caseId)
        );
    }

    @PatchMapping("/{caseId}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<CrmCaseResponse> assign(
            @PathVariable UUID caseId,
            @Valid @RequestBody AssignCrmCaseRequest request
    ) {
        return ResponseFactory.success(
                "CRM case assigned successfully",
                crmCaseService.assign(caseId, request)
        );
    }

    @PatchMapping("/{caseId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmCaseResponse> updateStatus(
            @PathVariable UUID caseId,
            @Valid @RequestBody UpdateCrmCaseStatusRequest request
    ) {
        return ResponseFactory.success(
                "CRM case status updated successfully",
                crmCaseService.updateStatus(caseId, request)
        );
    }

    @PostMapping("/{caseId}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmNoteResponse> addNote(
            @PathVariable UUID caseId,
            @Valid @RequestBody CreateCrmNoteRequest request
    ) {
        return ResponseFactory.success(
                "CRM note added successfully",
                crmCaseService.addNote(caseId, request)
        );
    }

    @PostMapping("/{caseId}/follow-up")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFollowUpResponse> createFollowUp(
            @PathVariable UUID caseId,
            @Valid @RequestBody CreateFollowUpRequest request
    ) {
        return ResponseFactory.success(
                "CRM follow-up scheduled successfully",
                crmCaseService.createFollowUp(caseId, request)
        );
    }
}