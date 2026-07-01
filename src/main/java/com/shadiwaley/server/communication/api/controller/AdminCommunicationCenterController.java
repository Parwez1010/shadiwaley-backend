package com.shadiwaley.server.communication.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.communication.domain.CommunicationQueueType;
import com.shadiwaley.server.communication.dto.request.BulkCommunicationActionRequest;
import com.shadiwaley.server.communication.dto.response.*;
import com.shadiwaley.server.communication.application.service.CommunicationCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/communication-center")
@RequiredArgsConstructor
public class AdminCommunicationCenterController {

    private final CommunicationCenterService communicationCenterService;

    @GetMapping("/dashboard")
    public ApiResponse<CommunicationDashboardResponse> getDashboard() {
        return ResponseFactory.success(
                "Communication center dashboard fetched successfully",
                communicationCenterService.getDashboard()
        );
    }

    @GetMapping("/my-queue")
    public ApiResponse<CommunicationQueueResponse> getMyQueue(
            @RequestParam(defaultValue = "ASSIGNED_TO_ME") CommunicationQueueType queueType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Communication center queue fetched successfully",
                communicationCenterService.getMyQueue(queueType, page, size)
        );
    }

    @GetMapping("/activity")
    public ApiResponse<CommunicationActivityPageResponse> getActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Communication center activity fetched successfully",
                communicationCenterService.getActivity(page, size)
        );
    }

    @GetMapping("/search")
    public ApiResponse<CommunicationSearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Communication search completed successfully",
                communicationCenterService.search(query, page, size)
        );
    }

    @PostMapping("/bulk-action")
    public ApiResponse<BulkCommunicationActionResponse> bulkAction(
            @Valid
            @RequestBody
            BulkCommunicationActionRequest request
    ) {

        return ResponseFactory.success(
                "Bulk action completed successfully",
                communicationCenterService.bulkAction(request)
        );

    }

    @GetMapping("/permissions")
    public ApiResponse<CommunicationPermissionResponse> getPermissions() {
        return ResponseFactory.success(
                "Communication center permissions fetched successfully",
                communicationCenterService.getPermissions()
        );
    }
}