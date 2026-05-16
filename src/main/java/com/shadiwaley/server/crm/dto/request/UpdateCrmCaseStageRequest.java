package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmCaseStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCrmCaseStageRequest {

    @NotNull(message = "Stage is required")
    private CrmCaseStage stage;

    @Size(max = 300)
    private String note;
}