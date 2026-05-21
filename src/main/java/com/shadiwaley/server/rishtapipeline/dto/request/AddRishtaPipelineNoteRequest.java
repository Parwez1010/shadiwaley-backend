package com.shadiwaley.server.rishtapipeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddRishtaPipelineNoteRequest {

    @NotBlank
    private String note;
}