package com.shadiwaley.server.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class
ReportChatMessageRequest {

    @NotBlank(message = "Reason is required")
    @Size(max = 100)
    private String reason;

    @Size(max = 500)
    private String details;
}