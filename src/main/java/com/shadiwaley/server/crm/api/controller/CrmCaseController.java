package com.shadiwaley.server.crm.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.crm.application.service.CrmCaseService;
import com.shadiwaley.server.crm.application.service.CrmDashboardService;
import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.dto.request.*;
import com.shadiwaley.server.crm.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/crm/cases")
@RequiredArgsConstructor
public class CrmCaseController {

    private final CrmCaseService crmCaseService;
    private final CrmDashboardService crmDashboardService;

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

    @PatchMapping("/{caseId}/stage")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmCaseResponse> updateStage(
            @PathVariable UUID caseId,
            @Valid @RequestBody UpdateCrmStageRequest request
    ) {
        return ResponseFactory.success(
                "CRM case stage updated successfully",
                crmCaseService.updateStage(caseId, request)
        );
    }

    @PostMapping("/{caseId}/follow-ups")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFollowUpResponse> createFollowUpPlural(
            @PathVariable UUID caseId,
            @Valid @RequestBody CreateFollowUpRequest request
    ) {
        return ResponseFactory.success(
                "CRM follow-up scheduled successfully",
                crmCaseService.createFollowUp(caseId, request)
        );
    }

    @PatchMapping("/{caseId}/follow-ups/{followUpId}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFollowUpResponse> completeFollowUp(
            @PathVariable UUID caseId,
            @PathVariable UUID followUpId,
            @Valid @RequestBody CompleteFollowUpRequest request
    ) {
        return ResponseFactory.success(
                "CRM follow-up completed successfully",
                crmCaseService.completeFollowUp(caseId, followUpId, request)
        );
    }

    @GetMapping("/{caseId}/timeline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<List<CrmTimelineResponse>> getTimeline(
            @PathVariable UUID caseId
    ) {
        return ResponseFactory.success(
                "CRM case timeline fetched successfully",
                crmCaseService.getTimeline(caseId)
        );
    }
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmDashboardResponse> getDashboard() {

        return ResponseFactory.success(
                "CRM dashboard fetched successfully",
                crmDashboardService.getDashboard()
        );
    }

    @PatchMapping("/{caseId}/follow-ups/{followUpId}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFollowUpResponse> rescheduleFollowUp(
            @PathVariable UUID caseId,
            @PathVariable UUID followUpId,
            @Valid @RequestBody RescheduleFollowUpRequest request
    ) {
        return ResponseFactory.success(
                "Follow-up rescheduled successfully",
                crmCaseService.rescheduleFollowUp(caseId, followUpId, request)
        );
    }

}