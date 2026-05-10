package com.shadiwaley.server.media.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaViewResponse {
    private String fileName;
    private String contentType;
    private byte[] content;
}