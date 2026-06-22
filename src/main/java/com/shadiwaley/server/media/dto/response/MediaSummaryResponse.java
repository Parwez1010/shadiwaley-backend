package com.shadiwaley.server.media.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaSummaryResponse {

    private Long totalFiles;

    private Long profilePhotos;

    private Long galleryPhotos;

    private Long documents;

    private Long chatImages;

    private Long chatDocuments;

    private Long pendingReview;

    private Long approved;

    private Long rejected;
}