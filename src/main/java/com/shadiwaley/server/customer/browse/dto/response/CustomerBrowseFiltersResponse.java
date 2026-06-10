package com.shadiwaley.server.customer.browse.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerBrowseFiltersResponse {

    private List<String> states;
    private List<String> districts;
    private List<String> maslak;
    private List<String> education;
    private List<String> familyTypes;
    private List<String> professionTypes;
    private List<String> quranLevels;
    private List<String> namaazRegularity;
    private List<String> maritalStatuses;

    private Short minAge;
    private Short maxAge;

    private Integer minIncome;
    private Integer maxIncome;

    private Integer minMehr;
    private Integer maxMehr;
}