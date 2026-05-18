package com.shadiwaley.server.proposal.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.proposal.application.service.ProposalDispatchService;
import com.shadiwaley.server.proposal.application.service.ProposalQueryService;
import com.shadiwaley.server.proposal.application.service.ProposalStatusService;
import com.shadiwaley.server.proposal.domain.ProposalDirection;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.dto.request.SendProposalRequest;
import com.shadiwaley.server.proposal.dto.request.UpdateProposalStatusRequest;
import com.shadiwaley.server.proposal.dto.response.ProposalDetailResponse;
import com.shadiwaley.server.proposal.dto.response.ProposalPageResponse;
import com.shadiwaley.server.proposal.dto.response.ProposalResponse;
import com.shadiwaley.server.proposal.dto.response.ProposalStatusUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalDispatchService proposalDispatchService;
    private final ProposalQueryService proposalQueryService;
    private final ProposalStatusService proposalStatusService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<ProposalResponse> sendProposal(
            @Valid @RequestBody SendProposalRequest request
    ) {
        return ResponseFactory.success(
                "Proposal sent successfully",
                proposalDispatchService.sendProposal(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<ProposalPageResponse> getProposals(
            @RequestParam(required = false) UUID profileId,
            @RequestParam(required = false) UUID crmCaseId,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(defaultValue = "BOTH") ProposalDirection direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseFactory.success(
                "Proposals fetched successfully",
                proposalQueryService.getProposals(
                        profileId,
                        crmCaseId,
                        status,
                        direction,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{proposalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<ProposalDetailResponse> getProposalDetail(
            @PathVariable UUID proposalId
    ) {
        return ResponseFactory.success(
                "Proposal detail fetched successfully",
                proposalQueryService.getProposalDetail(proposalId)
        );
    }

    @PatchMapping("/{proposalId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<ProposalStatusUpdateResponse> updateProposalStatus(
            @PathVariable UUID proposalId,
            @Valid @RequestBody UpdateProposalStatusRequest request
    ) {
        return ResponseFactory.success(
                "Proposal status updated successfully",
                proposalStatusService.updateStatus(proposalId, request)
        );
    }
}