package com.shadiwaley.server.crm.dto.request;

import com.shadiwaley.server.crm.domain.CrmCaseStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCrmStageRequest {

    @NotNull
    private CrmCaseStage stage;

    @Size(max = 1000)
    private String note;
}