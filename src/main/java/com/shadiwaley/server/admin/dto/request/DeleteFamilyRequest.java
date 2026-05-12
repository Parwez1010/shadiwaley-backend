package com.shadiwaley.server.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteFamilyRequest {

    @NotBlank(message = "Delete reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}