package com.shadiwaley.server.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<ApiError> errors;
    private Meta meta;

    @Getter
    @Builder
    public static class Meta {
        private Instant timestamp;
        private UUID requestId;
    }
}