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
    private MediaType mediaType;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private boolean primary;
    private MediaVisibility visibility;
    private WhatsappConsent whatsappConsent;
    private MediaReviewStatus reviewStatus;
    private Instant uploadedAt;
}