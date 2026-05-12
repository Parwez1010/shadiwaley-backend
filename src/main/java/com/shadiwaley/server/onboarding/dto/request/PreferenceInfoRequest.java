package com.shadiwaley.server.onboarding.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreferenceInfoRequest {
    private String preferredMaslak;
    private String preferredState;
    private String preferredDistrict;
    private Short minAge;
    private Short maxAge;
    private String preferredEducation;
    private String preferredFamilyType;
    private Boolean requireImamRef;
    private Boolean requireIdVerified;
    private String preferredCaste;
}