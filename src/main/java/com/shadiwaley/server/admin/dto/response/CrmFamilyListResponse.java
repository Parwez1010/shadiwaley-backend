package com.shadiwaley.server.admin.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmFamilyListResponse {

    private UUID userId;
    private UUID profileId;
    private String displayId;

    private String parentName;
    private String parentRelation;
    private String phone;

    private String candidateName;
    private Short age;
    private UserSide side;

    private String district;
    private String state;
    private String maslak;

    private String mode;
    private String planType;
    private String planDisplayName;

    private ProfileStatus profileStatus;
    private String displayStatus;
    private Short completionPct;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    private Instant createdAt;
    private Instant updatedAt;
}