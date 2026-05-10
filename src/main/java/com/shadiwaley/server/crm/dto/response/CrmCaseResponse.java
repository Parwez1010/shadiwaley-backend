package com.shadiwaley.server.crm.dto.response;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.domain.CrmCaseType;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmCaseResponse {

    private UUID caseId;

    private UUID userId;
    private UUID profileId;
    private String displayId;

    private String phone;
    private UserSide side;
    private String candidateName;

    private String district;
    private String state;
    private String maslak;

    private Short completionPct;
    private ProfileStatus profileStatus;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    private CrmCaseType caseType;
    private CrmCaseStatus status;
    private CrmCasePriority priority;

    private String source;
    private String summary;
    private String lastOutcome;

    private Instant nextFollowUpAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}