package com.shadiwaley.server.onboarding.dto.request;

import lombok.Getter;
import lombok.Setter;

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
}