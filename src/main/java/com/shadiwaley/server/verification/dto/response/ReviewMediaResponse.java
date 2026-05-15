package com.shadiwaley.server.verification.dto.response;

import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReviewMediaResponse {

    private UUID mediaId;
    private UUID documentId;

    private MediaType documentType;
    private String fileName;

    private String adminPreviewUrl;

    private MediaReviewStatus verificationStatus;
    private String rejectedReason;

    private Instant uploadedAt;
}