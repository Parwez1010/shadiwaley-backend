package com.shadiwaley.server.chat.dto.response;

import com.shadiwaley.server.media.domain.MediaType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AdminChatMediaUploadResponse {

    private UUID mediaFileId;

    private String fileName;

    private Long fileSizeBytes;

    private String contentType;

    private MediaType mediaType;

    private String mediaPreviewUrl;
}