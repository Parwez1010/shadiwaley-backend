package com.shadiwaley.server.media.dto.response;

import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.domain.MediaVisibility;
import com.shadiwaley.server.media.domain.WhatsappConsent;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MediaUploadResponse {

    private UUID mediaId;
    private UUID userId;
    private UUID profileId;

    private MediaType mediaType;
    private String fileName;
    private String contentType;
    private Long size;
    private Long fileSizeBytes;

    private Boolean primary;
    private MediaVisibility visibility;
    private WhatsappConsent whatsappConsent;
    private MediaReviewStatus verificationStatus;
    private MediaReviewStatus reviewStatus;

    private String rejectedReason;
    private String adminPreviewUrl;

    private Instant uploadedAt;
    private Instant createdAt;
}