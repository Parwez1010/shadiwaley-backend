package com.shadiwaley.server.profile.dto.response;

import com.shadiwaley.server.profile.domain.BodyType;
import com.shadiwaley.server.profile.domain.Diet;
import com.shadiwaley.server.profile.domain.ExerciseFrequency;
import com.shadiwaley.server.profile.domain.FamilyStatus;
import com.shadiwaley.server.profile.domain.FamilyValues;
import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class ProfileDetailResponse {

    private UUID profileId;
    private String displayId;
    private UserSide side;

    private String firstName;
    private Short candidateAge;
    private Short candidateHeightCm;

    private String district;
    private String state;
    private String maslak;

    private String religion;
    private MaritalStatus maritalStatus;
    private String education;
    private String quranLevel;
    private String namaazRegularity;
    private Boolean previouslyMarried;

    private String professionType;
    private String professionTitle;

    private Integer mehrOffered;
    private Integer mehrMinimumExpected;

    private String houseType;
    private String familyType;
    private String expectationsText;

    private boolean hasApprovedPhoto;

    private MatchBreakdownResponse match;

    // NEW — extended basic info
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String complexion;
    private BodyType bodyType;
    private String motherTongue;
    private Set<String> languagesKnown;
    private String sect;

    // NEW — lifestyle
    private Diet diet;
    private Boolean smoker;
    private Boolean drinker;
    private ExerciseFrequency exerciseFrequency;
    private Boolean wearsHijab;

    // NEW — family
    private FamilyStatus familyStatus;
    private FamilyValues familyValues;
    private Short brothersCount;
    private Short sistersCount;

    // NEW — interests
    private Set<String> interests;
}