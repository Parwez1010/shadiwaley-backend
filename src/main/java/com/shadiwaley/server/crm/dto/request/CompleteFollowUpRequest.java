package com.shadiwaley.server.crm.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteFollowUpRequest {

    @Size(max = 300)
    private String note;
}