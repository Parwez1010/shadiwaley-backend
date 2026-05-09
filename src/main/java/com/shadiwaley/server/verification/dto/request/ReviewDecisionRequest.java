package com.shadiwaley.server.verification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDecisionRequest {

    @NotBlank(message = "Review note is required")
    private String note;
}