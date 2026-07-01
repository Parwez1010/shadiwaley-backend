package com.shadiwaley.server.communication.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommunicationActivityPageResponse {

    private List<CommunicationActivityResponse> items;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;
}