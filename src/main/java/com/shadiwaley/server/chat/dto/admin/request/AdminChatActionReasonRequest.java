package com.shadiwaley.server.chat.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminChatActionReasonRequest {

    @NotBlank
    private String reason;
}