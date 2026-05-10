package com.shadiwaley.server.rishta.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.rishta.application.service.RishtaRequestService;
import com.shadiwaley.server.rishta.dto.request.CreateRishtaRequest;
import com.shadiwaley.server.rishta.dto.response.RishtaRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rishta")
@RequiredArgsConstructor
public class RishtaRequestController {

    private final RishtaRequestService rishtaRequestService;

    @PostMapping("/request")
    public ApiResponse<Void> sendRequest(
            @Valid @RequestBody CreateRishtaRequest request
    ) {

        rishtaRequestService.sendRequest(request);

        return ResponseFactory.success(
                "Rishta request sent successfully",
                null
        );
    }

    @GetMapping("/sent")
    public ApiResponse<List<RishtaRequestResponse>> sent() {

        return ResponseFactory.success(
                "Sent rishta requests fetched successfully",
                rishtaRequestService.getSentRequests()
        );
    }

    @GetMapping("/received")
    public ApiResponse<List<RishtaRequestResponse>> received() {

        return ResponseFactory.success(
                "Received rishta requests fetched successfully",
                rishtaRequestService.getReceivedRequests()
        );
    }

    @PostMapping("/{requestId}/accept")
    public ApiResponse<Void> accept(
            @PathVariable UUID requestId
    ) {

        rishtaRequestService.accept(requestId);

        return ResponseFactory.success(
                "Rishta request accepted successfully",
                null
        );
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<Void> reject(
            @PathVariable UUID requestId
    ) {

        rishtaRequestService.reject(requestId);

        return ResponseFactory.success(
                "Rishta request rejected successfully",
                null
        );
    }
}