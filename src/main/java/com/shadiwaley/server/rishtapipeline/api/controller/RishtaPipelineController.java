package com.shadiwaley.server.rishtapipeline.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.application.service.RishtaPipelineService;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import com.shadiwaley.server.rishtapipeline.dto.request.AddRishtaPipelineNoteRequest;
import com.shadiwaley.server.rishtapipeline.dto.request.CreatePipelineFollowUpRequest;
import com.shadiwaley.server.rishtapipeline.dto.request.UpdateRishtaPipelineStageRequest;
import com.shadiwaley.server.rishtapipeline.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/rishta-pipeline")
@RequiredArgsConstructor
public class RishtaPipelineController {

    private final RishtaPipelineService pipelineService;


    @GetMapping("/{proposalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RishtaPipelineDetailResponse> getPipelineItem(
            @PathVariable UUID proposalId
    ) {
        return ResponseFactory.success(
                "Pipeline detail fetched successfully",
                pipelineService.getPipelineDetail(proposalId)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RishtaPipelinePageResponse> getPipeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RishtaPipelineStage stage,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(required = false) UUID crmEmployeeId,
            @RequestParam(required = false) UUID fromProfileId,
            @RequestParam(required = false) UUID toProfileId,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String side,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String subscriptionStatus,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "lastActivityAt,desc") String sort
    ) {
        return ResponseFactory.success(
                "Rishta pipeline fetched successfully",
                pipelineService.getPipeline(
                        page,
                        size,
                        search,
                        stage,
                        status,
                        crmEmployeeId,
                        fromProfileId,
                        toProfileId,
                        district,
                        side,
                        planCode,
                        paymentStatus,
                        subscriptionStatus,
                        overdueOnly,
                        fromDate,
                        toDate,
                        sort
                )
        );
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RishtaPipelineSummaryResponse> getSummary(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID crmEmployeeId
    ) {
        return ResponseFactory.success(
                "Pipeline summary fetched successfully",
                pipelineService.getSummary(fromDate, toDate, crmEmployeeId)
        );
    }

    @GetMapping("/kanban")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RishtaPipelineKanbanResponse> getKanban(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID crmEmployeeId
    ) {
        return ResponseFactory.success(
                "Pipeline kanban fetched successfully",
                pipelineService.getKanban(
                        fromDate,
                        toDate,
                        crmEmployeeId
                )
        );
    }

    @PatchMapping("/{proposalId}/stage")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RishtaPipelineStageUpdateResponse> updateStage(
            @PathVariable UUID proposalId,
            @Valid @RequestBody UpdateRishtaPipelineStageRequest request
    ) {
        return ResponseFactory.success(
                "Pipeline stage updated successfully",
                pipelineService.updateStage(proposalId, request)
        );
    }

    @PostMapping("/{proposalId}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RishtaPipelineNoteResponse> addNote(
            @PathVariable UUID proposalId,
            @Valid @RequestBody AddRishtaPipelineNoteRequest request
    ) {
        return ResponseFactory.success(
                "Pipeline note added successfully",
                pipelineService.addNote(proposalId, request)
        );
    }

    @PostMapping("/{proposalId}/follow-up")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<PipelineFollowUpResponse> createFollowUp(
            @PathVariable UUID proposalId,
            @Valid @RequestBody CreatePipelineFollowUpRequest request
    ) {
        return ResponseFactory.success(
                "Pipeline follow-up created successfully",
                pipelineService.createFollowUp(proposalId, request)
        );
    }

}