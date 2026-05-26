package com.shadiwaley.server.chat.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminChatNoteRequest {

    @NotBlank
    @Size(max = 2000)
    private String note;
}