package com.shadiwaley.server.autopilot.api.controller;

import com.shadiwaley.server.autopilot.application.service.AutopilotDispatchService;
import com.shadiwaley.server.autopilot.domain.AutopilotBatchStatus;
import com.shadiwaley.server.autopilot.domain.AutopilotQueueStatus;
import com.shadiwaley.server.autopilot.dto.request.*;
import com.shadiwaley.server.autopilot.dto.response.*;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/autopilot")
@RequiredArgsConstructor
public class AutopilotDispatchController {

    private final AutopilotDispatchService autopilotDispatchService;

    @GetMapping("/dispatch-queue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<AutopilotQueuePageResponse> getQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AutopilotQueueStatus status,
            @RequestParam(required = false) UUID assignedEmployeeId,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String side,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String subscriptionStatus,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "priorityScore,desc") String sort
    ) {
        return ResponseFactory.success(
                "Autopilot dispatch queue fetched successfully",
                autopilotDispatchService.getQueue(
                        page,
                        size,
                        search,
                        status,
                        assignedEmployeeId,
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
    public ApiResponse<AutopilotSummaryResponse> getSummary(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID assignedEmployeeId
    ) {
        return ResponseFactory.success(
                "Autopilot summary fetched successfully",
                autopilotDispatchService.getSummary(
                        fromDate,
                        toDate,
                        assignedEmployeeId
                )
        );
    }

    @GetMapping("/dispatch-queue/{queueId}/suggestions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<AutopilotDispatchSuggestionsResponse> getSuggestions(
            @PathVariable UUID queueId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") Boolean includeAlreadySent
    ) {
        return ResponseFactory.success(
                "Dispatch suggestions fetched successfully",
                autopilotDispatchService.getSuggestions(
                        queueId,
                        limit,
                        includeAlreadySent
                )
        );
    }

    @PostMapping("/dispatches/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<DispatchPreviewResponse> createPreview(
            @Valid @RequestBody CreateDispatchPreviewRequest request
    ) {
        return ResponseFactory.success(
                "Dispatch preview generated successfully",
                autopilotDispatchService.createPreview(request)
        );
    }

    @PostMapping("/dispatches/{draftId}/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<SendDispatchResponse> sendDispatch(
            @PathVariable UUID draftId,
            @Valid @RequestBody SendDispatchRequest request
    ) {
        return ResponseFactory.success(
                "Dispatch marked as sent successfully",
                autopilotDispatchService.sendDispatch(draftId, request)
        );
    }

    @PostMapping("/dispatch-queue/{queueId}/skip")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<QueueActionResponse> skipQueue(
            @PathVariable UUID queueId,
            @Valid @RequestBody SkipDispatchQueueRequest request
    ) {
        return ResponseFactory.success(
                "Dispatch skipped successfully",
                autopilotDispatchService.skipQueue(queueId, request)
        );
    }

    @PostMapping("/dispatch-queue/{queueId}/postpone")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<QueueActionResponse> postponeQueue(
            @PathVariable UUID queueId,
            @Valid @RequestBody PostponeDispatchQueueRequest request
    ) {
        return ResponseFactory.success(
                "Dispatch postponed successfully",
                autopilotDispatchService.postponeQueue(queueId, request)
        );
    }

    @GetMapping("/dispatches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<DispatchHistoryPageResponse> getDispatchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AutopilotBatchStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseFactory.success(
                "Dispatch history fetched successfully",
                autopilotDispatchService.getDispatchHistory(
                        page,
                        size,
                        search,
                        status,
                        fromDate,
                        toDate,
                        sort
                )
        );
    }

    @GetMapping("/dispatches/{dispatchBatchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<DispatchDetailResponse> getDispatchDetail(
            @PathVariable UUID dispatchBatchId
    ) {
        return ResponseFactory.success(
                "Dispatch detail fetched successfully",
                autopilotDispatchService.getDispatchDetail(dispatchBatchId)
        );
    }

    @PatchMapping("/dispatch-items/{dispatchItemId}/response")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')")
    public ApiResponse<RecordDispatchResponseResponse> recordResponse(
            @PathVariable UUID dispatchItemId,
            @Valid @RequestBody RecordDispatchResponseRequest request
    ) {
        return ResponseFactory.success(
                "Dispatch response recorded successfully",
                autopilotDispatchService.recordResponse(dispatchItemId, request)
        );
    }

    @PostMapping("/dispatch-queue/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<GenerateDispatchQueueResponse> generateQueue(
            @RequestBody GenerateDispatchQueueRequest request
    ) {
        return ResponseFactory.success(
                "Dispatch queue generated successfully",
                autopilotDispatchService.generateQueue(request)
        );
    }

}