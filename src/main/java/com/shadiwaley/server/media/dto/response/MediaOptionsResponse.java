package com.shadiwaley.server.media.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MediaOptionsResponse {

    private List<String> mediaTypes;
    private List<String> whatsappConsents;
    private List<String> mediaVisibilities;
    private List<String> reviewStatuses;
}