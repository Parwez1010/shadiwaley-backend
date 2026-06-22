package com.shadiwaley.server.support.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SupportTicketReplyRequest {

    @NotBlank
    @Size(max = 3000)
    private String message;
    private Boolean internalNote;

    private UUID mediaFileId;
}