package com.shadiwaley.server.communication.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkCommunicationActionResponse {

    private int processed;

    private int success;

    private int failed;
}