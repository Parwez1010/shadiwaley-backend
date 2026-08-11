package com.shadiwaley.server.onboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OnboardingOptionsResponse {
    private List<String> parentRelation;
    private List<String> education;
    private List<String> quranLevel;
    private List<String> namaazRegularity;
    private List<String> professionType;
    private List<String> houseType;
    private List<String> familyType;
    private List<String> preferredEducation;
    private List<String> preferredFamilyType;
    private List<String> mediaType;
    private List<String> whatsappConsent;
    private List<String> caste;
    private List<String> preferredCaste;
    private List<String> maslak;
    private List<String> preferredMaslak;
    private List<String> state;
    private List<String> district;
    private List<String> preferredState;
    private List<String> preferredDistrict;

    // NEW
    private List<String> bodyType;
    private List<String> diet;
    private List<String> exerciseFrequency;
    private List<String> familyStatus;
    private List<String> familyValues;
    private List<String> bloodGroup;
    private List<String> complexion;
    private List<String> motherTongue;
    private List<String> languagesKnown;
    private List<String> interests;
}