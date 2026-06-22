package com.shadiwaley.server.support.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SupportAssigneeResponse {

    private UUID employeeId;

    private String fullName;

    private String email;

    private String role;

    private String assignedDistrict;

    private boolean active;
}