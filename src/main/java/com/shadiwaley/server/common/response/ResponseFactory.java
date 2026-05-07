package com.shadiwaley.server.common.response;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ResponseFactory {

    private ResponseFactory() {
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .errors(Collections.emptyList())
                .meta(meta())
                .build();
    }

    public static <T> ApiResponse<T> failure(String message, List<ApiError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .errors(errors)
                .meta(meta())
                .build();
    }

    private static ApiResponse.Meta meta() {
        return ApiResponse.Meta.builder()
                .timestamp(Instant.now())
                .requestId(UUID.randomUUID())
                .build();
    }
}