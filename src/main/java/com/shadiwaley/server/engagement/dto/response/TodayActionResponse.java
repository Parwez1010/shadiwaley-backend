package com.shadiwaley.server.engagement.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodayActionResponse {

    private String title;

    private String priority;

    private String message;

    private String actionCode;
}