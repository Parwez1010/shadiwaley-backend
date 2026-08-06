package com.shadiwaley.server.onboarding.dto.request;

import com.shadiwaley.server.profile.domain.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class ProfileInfoRequest {
    private String candidateFirstName;
    private Short candidateAge;
    private Short candidateHeightCm;
    private String education;
    private String quranLevel;
    private String namaazRegularity;
    private Boolean previouslyMarried;
    private String professionType;
    private String professionTitle;
    private Integer monthlyIncome;
    private Integer mehrOffered;
    private Integer mehrMinimumExpected;
    private String houseType;
    private String familyType;
    private String expectationsText;
    private String religion;

    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String complexion;
    private BodyType bodyType;
    private String motherTongue;
    private Set<String> languagesKnown;
    private String sect;

    private Diet diet;
    private Boolean smoker;
    private Boolean drinker;
    private ExerciseFrequency exerciseFrequency;
    private Boolean wearsHijab;

    private FamilyStatus familyStatus;
    private FamilyValues familyValues;
    private Short brothersCount;
    private Short sistersCount;

    private Set<String> interests;

    private com.shadiwaley.server.profile.domain.MaritalStatus maritalStatus;
}