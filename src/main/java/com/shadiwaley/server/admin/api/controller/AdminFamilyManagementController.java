package com.shadiwaley.server.admin.api.controller;

import com.shadiwaley.server.admin.application.service.AdminFamilyService;
import com.shadiwaley.server.admin.dto.request.AssignFamilyCrmRequest;
import com.shadiwaley.server.admin.dto.request.CreateAdminFamilyRequest;
import com.shadiwaley.server.admin.dto.request.DeleteFamilyRequest;
import com.shadiwaley.server.admin.dto.request.SubmitFamilyReviewRequest;
import com.shadiwaley.server.admin.dto.request.UpdateAdminFamilyStatusRequest;
import com.shadiwaley.server.admin.dto.response.CrmFamilyDetailResponse;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.media.application.service.MediaService;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.domain.WhatsappConsent;
import com.shadiwaley.server.media.dto.request.UpdateWhatsappConsentRequest;
import com.shadiwaley.server.media.dto.response.MediaUploadResponse;
import com.shadiwaley.server.media.dto.response.MediaViewResponse;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/crm/families")
@RequiredArgsConstructor
public class AdminFamilyManagementController {

    private final AdminFamilyService adminFamilyService;
    private final MediaService mediaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFamilyDetailResponse> createFamily(
            @Valid @RequestBody CreateAdminFamilyRequest request
    ) {
        return ResponseFactory.success(
                "Family created successfully",
                adminFamilyService.createFamily(request)
        );
    }

    @PatchMapping("/{userId}/onboarding")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFamilyDetailResponse> updateFamilyOnboarding(
            @PathVariable UUID userId,
            @Valid @RequestBody OnboardingProfileUpsertRequest request
    ) {
        return ResponseFactory.success(
                "Family updated successfully",
                adminFamilyService.updateOnboarding(userId, request)
        );
    }

    @PostMapping("/{userId}/submit-review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CrmFamilyDetailResponse> submitFamilyForReview(
            @PathVariable UUID userId,
            @Valid @RequestBody SubmitFamilyReviewRequest request
    ) {
        return ResponseFactory.success(
                "Family submitted for review successfully",
                adminFamilyService.submitForReview(userId, request)
        );
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<CrmFamilyDetailResponse> updateFamilyStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAdminFamilyStatusRequest request
    ) {
        return ResponseFactory.success(
                "Family status updated successfully",
                adminFamilyService.updateStatus(userId, request)
        );
    }

    @PatchMapping("/{userId}/assign-crm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<CrmFamilyDetailResponse> assignCrm(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignFamilyCrmRequest request
    ) {
        return ResponseFactory.success(
                "CRM assigned successfully",
                adminFamilyService.assignCrm(userId, request)
        );
    }

    @PostMapping("/{userId}/delete")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> deleteFamily(
            @PathVariable UUID userId,
            @Valid @RequestBody DeleteFamilyRequest request
    ) {
        adminFamilyService.deleteFamily(userId, request);

        return ResponseFactory.success(
                "Family permanently deleted successfully",
                null
        );
    }

    @PostMapping("/{userId}/media")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<MediaUploadResponse> uploadFamilyMedia(
            @PathVariable UUID userId,
            @RequestParam MediaType mediaType,
            @RequestParam(defaultValue = "false") boolean primary,
            @RequestParam(defaultValue = "NEVER") WhatsappConsent whatsappConsent,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseFactory.success(
                "Family media uploaded successfully",
                mediaService.uploadMediaForUser(userId, mediaType, primary, whatsappConsent, file)
        );
    }

    @GetMapping("/{userId}/media")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT', 'VERIFIER')")
    public ApiResponse<List<MediaUploadResponse>> getFamilyMedia(
            @PathVariable UUID userId
    ) {
        return ResponseFactory.success(
                "Family media fetched successfully",
                mediaService.getFamilyMedia(userId)
        );
    }

    @GetMapping("/{userId}/media/{mediaId}/view")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT', 'VERIFIER')")
    public ResponseEntity<byte[]> viewFamilyMedia(
            @PathVariable UUID userId,
            @PathVariable UUID mediaId
    ) {
        MediaViewResponse response = mediaService.viewFamilyMedia(userId, mediaId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + response.getFileName() + "\""
                )
                .contentType(org.springframework.http.MediaType.parseMediaType(response.getContentType()))
                .body(response.getContent());
    }

    @DeleteMapping("/{userId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<Void> deleteFamilyMedia(
            @PathVariable UUID userId,
            @PathVariable UUID mediaId
    ) {
        mediaService.deleteFamilyMedia(userId, mediaId);

        return ResponseFactory.success(
                "Family media deleted successfully",
                null
        );
    }

    @PatchMapping("/{userId}/media/{mediaId}/primary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<MediaUploadResponse> setPrimaryFamilyMedia(
            @PathVariable UUID userId,
            @PathVariable UUID mediaId
    ) {
        return ResponseFactory.success(
                "Primary profile photo updated successfully",
                mediaService.setPrimaryFamilyMedia(userId, mediaId)
        );
    }

    @PatchMapping("/{userId}/media/{mediaId}/whatsapp-consent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<MediaUploadResponse> updateFamilyMediaWhatsappConsent(
            @PathVariable UUID userId,
            @PathVariable UUID mediaId,
            @Valid @RequestBody UpdateWhatsappConsentRequest request
    ) {
        return ResponseFactory.success(
                "WhatsApp consent updated successfully",
                mediaService.updateFamilyMediaWhatsappConsent(
                        userId,
                        mediaId,
                        request.getWhatsappConsent()
                )
        );
    }


}