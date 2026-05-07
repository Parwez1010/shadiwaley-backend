package com.shadiwaley.server.onboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissingFieldResponse {
    private String field;
    private String label;
    private String priority;
}