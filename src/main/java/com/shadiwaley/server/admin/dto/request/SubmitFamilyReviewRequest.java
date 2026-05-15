package com.shadiwaley.server.admin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitFamilyReviewRequest {

    @Size(max = 500)
    private String note;
}