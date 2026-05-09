package com.shadiwaley.server.profile.dto.request;

import com.shadiwaley.server.profile.domain.MaritalStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrowseProfileFilterRequest {

    private Integer page = 0;
    private Integer size = 10;

    private String country;
    private String state;
    private String district;
    private String maslak;

    private Short minAge;
    private Short maxAge;

    private String education;
    private String familyType;
    private String professionType;
    private String quranLevel;
    private String namaazRegularity;

    private MaritalStatus maritalStatus;

    private Integer minIncome;
    private Integer maxIncome;

    private Integer minMehr;
    private Integer maxMehr;
}