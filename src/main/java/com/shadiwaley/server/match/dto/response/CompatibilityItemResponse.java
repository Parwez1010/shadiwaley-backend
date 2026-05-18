package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompatibilityItemResponse {

    private boolean matched;
    private int score;
    private String message;
}