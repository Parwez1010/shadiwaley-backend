package com.shadiwaley.server.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PendingActionResponse {
    private String type;
    private String title;
    private String description;
    private String priority;
    private UUID referenceId;
    private String actionUrl;
}