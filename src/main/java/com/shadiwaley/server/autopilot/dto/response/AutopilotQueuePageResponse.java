package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AutopilotQueuePageResponse {

    private List<AutopilotQueueItemResponse> items;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}