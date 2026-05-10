package com.shadiwaley.server.admin.dto.response;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CrmFamilyDetailResponse {
    private UUID userId;
    private UUID profileId;
    private String displayId;

    private String phone;
    private UserSide side;
    private String accountStatus;

    private String candidateName;
    private Short age;
    private Short heightCm;

    private String education;
    private String quranLevel;
    private String namaazRegularity;
    private String professionType;
    private String professionTitle;
    private Integer monthlyIncome;
    private String familyType;
    private String expectationsText;

    private String parentName;
    private String parentPhone;
    private String parentRelation;
    private String district;
    private String state;
    private String maslak;
    private String imamReference;
    private String masjidName;

    private Short completionPct;
    private ProfileStatus profileStatus;

    private Instant createdAt;
    private Instant lastLoginAt;
}