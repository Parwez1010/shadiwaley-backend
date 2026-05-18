package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreRangeResponse {

    private String label;
    private int min;
    private int max;
}