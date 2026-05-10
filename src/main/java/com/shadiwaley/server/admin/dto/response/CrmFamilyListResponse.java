package com.shadiwaley.server.admin.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CrmFamilyListResponse {
    private UUID userId;
    private UUID profileId;
    private String displayId;

    private String phone;
    private UserSide side;

    private String candidateName;
    private Short age;

    private String parentName;
    private String district;
    private String state;
    private String maslak;

    private Short completionPct;
    private ProfileStatus profileStatus;
}