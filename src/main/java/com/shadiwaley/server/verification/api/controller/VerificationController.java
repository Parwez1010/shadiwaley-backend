package com.shadiwaley.server.verification.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.verification.application.service.VerificationService;
import com.shadiwaley.server.verification.dto.request.ReviewDecisionRequest;
import com.shadiwaley.server.verification.dto.response.ReviewQueueProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/review")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'CRM_AGENT', 'VERIFIER')")
    public ApiResponse<List<ReviewQueueProfileResponse>> getProfilesForReview() {
        return ResponseFactory.success(
                "Profiles fetched successfully",
                verificationService.getProfilesForReview()
        );
    }

    @PostMapping("/profile/{profileId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VERIFIER')")
    public ApiResponse<Void> approveProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        verificationService.approveProfile(profileId, request.getNote());
        return ResponseFactory.success("Profile approved successfully", null);
    }

    @PostMapping("/profile/{profileId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VERIFIER')")
    public ApiResponse<Void> rejectProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        verificationService.rejectProfile(profileId, request.getNote());
        return ResponseFactory.success("Profile rejected successfully", null);
    }

    @PostMapping("/media/{mediaId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VERIFIER')")
    public ApiResponse<Void> approveMedia(
            @PathVariable UUID mediaId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        verificationService.approveMedia(mediaId, request.getNote());
        return ResponseFactory.success("Media approved successfully", null);
    }

    @PostMapping("/media/{mediaId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VERIFIER')")
    public ApiResponse<Void> rejectMedia(
            @PathVariable UUID mediaId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        verificationService.rejectMedia(mediaId, request.getNote());
        return ResponseFactory.success("Media rejected successfully", null);
    }

}