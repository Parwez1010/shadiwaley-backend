package com.shadiwaley.server.media.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GroupedMediaResponse {
    private MediaUploadResponse profilePhoto;
    private List<MediaUploadResponse> galleryPhotos;
    private List<MediaUploadResponse> documents;
}