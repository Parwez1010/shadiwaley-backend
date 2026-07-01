package com.shadiwaley.server.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminChatInternalNoteRequest {

    @NotBlank
    @Size(max = 2000)
    private String note;
}