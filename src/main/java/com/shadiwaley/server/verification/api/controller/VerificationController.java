package com.shadiwaley.server.verification.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.media.application.service.MediaService;
import com.shadiwaley.server.media.dto.response.MediaViewResponse;
import com.shadiwaley.server.verification.application.service.VerificationService;
import com.shadiwaley.server.verification.dto.request.ReviewDecisionRequest;
import com.shadiwaley.server.verification.dto.response.ReviewProfileDetailResponse;
import com.shadiwaley.server.verification.dto.response.ReviewQueueProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.parser.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/review")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final MediaService mediaService;

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

    @GetMapping("/profile/{profileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VERIFIER')")
    public ApiResponse<ReviewProfileDetailResponse> getReviewProfileDetail(
            @PathVariable UUID profileId
    ) {
        return ResponseFactory.success(
                "Review profile detail fetched successfully",
                verificationService.getReviewProfileDetail(profileId)
        );
    }

    @GetMapping("/media/{mediaId}/view")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VERIFIER')")
    public ResponseEntity<byte[]> viewReviewMedia(@PathVariable UUID mediaId) {
        MediaViewResponse response = mediaService.viewAdminMedia(mediaId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + response.getFileName() + "\""
                )
                .contentType(org.springframework.http.MediaType.parseMediaType(response.getContentType()))
                .body(response.getContent());
    }

}