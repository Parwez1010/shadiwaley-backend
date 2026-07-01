package com.shadiwaley.server.communication.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommunicationSearchResponse {

    private String query;

    private List<CommunicationQueueItemResponse> items;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;
}