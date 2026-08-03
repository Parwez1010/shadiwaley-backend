package com.shadiwaley.server.profile.dto.response;

import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProfileCardResponse {
    private UUID profileId;
    private String displayId;
    private UserSide side;

    private String firstName;
    private String candidateFirstName; // optional, keep until clients migrate
    private Short candidateAge;
    private Short candidateHeightCm;

    private String district;
    private String state;
    private String maslak;

    private String education;
    private String quranLevel;
    private String namaazRegularity;
    private String professionType;
    private String professionTitle;
    private String familyType;

    private boolean hasApprovedPhoto;
    private MatchBreakdownResponse match;
    private String religion;
    private MaritalStatus maritalStatus;

    private String houseType;

    private Integer mehrOffered;

    private Integer mehrMinimumExpected;

    private String expectationsText;

    private boolean saved;

    private String proposalStatus;
}