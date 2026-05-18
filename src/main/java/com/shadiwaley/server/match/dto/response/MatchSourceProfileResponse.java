package com.shadiwaley.server.match.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MatchSourceProfileResponse {

    private UUID profileId;
    private UUID userId;
    private UUID crmCaseId;

    private String displayId;
    private String candidateName;

    private String parentName;
    private String parentPhone;

    private String side;
    private Integer age;

    private String district;
    private String state;
    private String caste;
    private String maslak;

    private String education;

    private ProfileStatus profileStatus;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
}