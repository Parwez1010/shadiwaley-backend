package com.shadiwaley.server.media.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.media.application.service.MediaService;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.domain.WhatsappConsent;
import com.shadiwaley.server.media.dto.request.UpdateConsentRequest;
import com.shadiwaley.server.media.dto.response.GroupedMediaResponse;
import com.shadiwaley.server.media.dto.response.MediaUploadResponse;
import com.shadiwaley.server.media.dto.response.MediaViewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ApiResponse<MediaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType,
            @RequestParam(value = "whatsappConsent", required = false) WhatsappConsent whatsappConsent
    ) {
        return ResponseFactory.success(
                "File uploaded successfully. It is pending review.",
                mediaService.upload(file, mediaType, whatsappConsent)
        );
    }

    @GetMapping("/my-files")
    public ApiResponse<List<MediaUploadResponse>> getMyFiles() {
        return ResponseFactory.success(
                "Files fetched successfully",
                mediaService.getMyFiles()
        );
    }

    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            @PathVariable UUID mediaId
    ) {

        mediaService.deleteMedia(mediaId);

        return ResponseFactory.success(
                "Media deleted successfully",
                null
        );
    }

    @PatchMapping("/{mediaId}/consent")
    public ApiResponse<MediaUploadResponse> updateConsent(
            @PathVariable UUID mediaId,
            @Valid @RequestBody UpdateConsentRequest request
    ) {

        return ResponseFactory.success(
                "Whatsapp consent updated successfully",
                mediaService.updateConsent(mediaId, request.getWhatsappConsent())
        );
    }

    @PatchMapping("/{mediaId}/make-primary")
    public ApiResponse<MediaUploadResponse> makePrimary(
            @PathVariable UUID mediaId
    ) {

        return ResponseFactory.success(
                "Primary photo updated successfully",
                mediaService.makePrimary(mediaId)
        );
    }

    @GetMapping("/grouped")
    public ApiResponse<GroupedMediaResponse> getGroupedMedia() {

        return ResponseFactory.success(
                "Grouped media fetched successfully",
                mediaService.getGroupedMedia()
        );
    }

    @GetMapping("/{mediaId}/view")
    public org.springframework.http.ResponseEntity<byte[]> viewMedia(
            @PathVariable UUID mediaId
    ) {
        MediaViewResponse response = mediaService.viewMedia(mediaId);

        return org.springframework.http.ResponseEntity.ok()
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + response.getFileName() + "\""
                )
                .contentType(org.springframework.http.MediaType.parseMediaType(response.getContentType()))
                .body(response.getContent());
    }

}