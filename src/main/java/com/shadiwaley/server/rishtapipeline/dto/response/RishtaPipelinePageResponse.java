package com.shadiwaley.server.rishtapipeline.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RishtaPipelinePageResponse {

    private List<RishtaPipelineItemResponse> items;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}