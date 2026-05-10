package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmNoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCrmNoteRequest {

    @NotNull(message = "Note type is required")
    private CrmNoteType noteType = CrmNoteType.INTERNAL_NOTE;

    @NotBlank(message = "Note is required")
    private String note;
}