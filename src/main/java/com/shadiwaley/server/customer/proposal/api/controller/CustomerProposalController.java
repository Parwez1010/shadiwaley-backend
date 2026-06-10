package com.shadiwaley.server.customer.proposal.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.customer.proposal.application.service.CustomerProposalService;
import com.shadiwaley.server.customer.proposal.dto.request.SendCustomerProposalRequest;
import com.shadiwaley.server.customer.proposal.dto.response.*;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/proposals")
@RequiredArgsConstructor
public class CustomerProposalController {

    private final CustomerProposalService customerProposalService;

    @GetMapping("/summary")
    public ApiResponse<CustomerProposalSummaryResponse> getSummary() {
        return ResponseFactory.success(
                "Proposal summary fetched successfully",
                customerProposalService.getSummary()
        );
    }

    @GetMapping("/sent")
    public ApiResponse<CustomerProposalPageResponse> getSent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RishtaRequestStatus status
    ) {
        return ResponseFactory.success(
                "Sent proposals fetched successfully",
                customerProposalService.getSent(page, size, status)
        );
    }

    @GetMapping("/received")
    public ApiResponse<CustomerProposalPageResponse> getReceived(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RishtaRequestStatus status
    ) {
        return ResponseFactory.success(
                "Received proposals fetched successfully",
                customerProposalService.getReceived(page, size, status)
        );
    }

    @GetMapping("/incoming")
    public ApiResponse<CustomerProposalPageResponse> getIncoming(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RishtaRequestStatus status
    ) {
        return ResponseFactory.success(
                "Incoming proposals fetched successfully",
                customerProposalService.getReceived(page, size, status)
        );
    }


    @GetMapping("/{requestId}")
    public ApiResponse<CustomerProposalDetailResponse> getDetail(
            @PathVariable UUID requestId
    ) {
        return ResponseFactory.success(
                "Proposal detail fetched successfully",
                customerProposalService.getDetail(requestId)
        );
    }

    @PostMapping("/{requestId}/accept")
    public ApiResponse<CustomerProposalDetailResponse> accept(
            @PathVariable UUID requestId
    ) {
        return ResponseFactory.success(
                "Proposal accepted successfully",
                customerProposalService.accept(requestId)
        );
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<CustomerProposalDetailResponse> reject(
            @PathVariable UUID requestId
    ) {
        return ResponseFactory.success(
                "Proposal rejected successfully",
                customerProposalService.reject(requestId)
        );
    }

    @PostMapping("/{requestId}/cancel")
    public ApiResponse<CustomerProposalDetailResponse> cancel(
            @PathVariable UUID requestId
    ) {
        return ResponseFactory.success(
                "Proposal cancelled successfully",
                customerProposalService.cancel(requestId)
        );
    }

    @PostMapping("/send")
    public ApiResponse<Void> sendProposal(
            @Valid @RequestBody SendCustomerProposalRequest request
    ) {
        customerProposalService.sendProposal(request);

        return ResponseFactory.success(
                "Proposal sent successfully",
                null
        );
    }

}