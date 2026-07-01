package com.shadiwaley.server.communication.dto.response;

import com.shadiwaley.server.communication.domain.CommunicationQueueType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommunicationQueueResponse {

    private CommunicationQueueType queueType;

    private List<CommunicationQueueItemResponse> items;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;
}