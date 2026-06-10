package com.shadiwaley.server.customer.browse.application.service;

import com.shadiwaley.server.customer.browse.dto.response.CustomerBrowseFiltersResponse;
import com.shadiwaley.server.profile.application.service.ProfileBrowseService;
import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.profile.dto.request.BrowseProfileFilterRequest;
import com.shadiwaley.server.profile.dto.response.BrowseProfilesResponse;
import com.shadiwaley.server.profile.dto.response.ProfileDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerBrowseService {

    private final ProfileBrowseService profileBrowseService;

    @Transactional(readOnly = true)
    public BrowseProfilesResponse browseProfiles(
            Integer page,
            Integer size,
            String country,
            String state,
            String district,
            String maslak,
            Short minAge,
            Short maxAge,
            String education,
            String familyType,
            String professionType,
            String quranLevel,
            String namaazRegularity,
            MaritalStatus maritalStatus,
            Integer minIncome,
            Integer maxIncome,
            Integer minMehr,
            Integer maxMehr
    ) {
        BrowseProfileFilterRequest filter = new BrowseProfileFilterRequest();

        filter.setPage(page);
        filter.setSize(size);
        filter.setCountry(country);
        filter.setState(state);
        filter.setDistrict(district);
        filter.setMaslak(maslak);
        filter.setMinAge(minAge);
        filter.setMaxAge(maxAge);
        filter.setEducation(education);
        filter.setFamilyType(familyType);
        filter.setProfessionType(professionType);
        filter.setQuranLevel(quranLevel);
        filter.setNamaazRegularity(namaazRegularity);
        filter.setMaritalStatus(maritalStatus);
        filter.setMinIncome(minIncome);
        filter.setMaxIncome(maxIncome);
        filter.setMinMehr(minMehr);
        filter.setMaxMehr(maxMehr);

        return profileBrowseService.browse(filter);
    }

    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileDetail(UUID profileId) {
        return profileBrowseService.getProfileDetail(profileId);
    }

    @Transactional(readOnly = true)
    public CustomerBrowseFiltersResponse getFilters() {
        return CustomerBrowseFiltersResponse.builder()
                .states(List.of("Bihar", "Jharkhand", "Uttar Pradesh", "Delhi", "West Bengal"))
                .districts(List.of("Patna", "Gaya", "Muzaffarpur", "Bhagalpur", "Darbhanga"))
                .maslak(List.of("Sunni", "Deobandi", "Barelvi", "Ahl-e-Hadith"))
                .education(List.of("Matric", "Intermediate", "Graduate", "Post Graduate", "B.Tech", "MBBS", "MBA"))
                .familyTypes(List.of("NUCLEAR", "JOINT"))
                .professionTypes(List.of("PRIVATE_JOB", "GOVERNMENT_JOB", "BUSINESS", "SELF_EMPLOYED", "STUDENT"))
                .quranLevels(List.of("CAN_READ", "HIFZ", "LEARNING", "BASIC"))
                .namaazRegularity(List.of("FIVE_TIMES", "SOMETIMES", "FRIDAY_ONLY", "LEARNING"))
                .maritalStatuses(
                        Arrays.stream(MaritalStatus.values())
                                .map(Enum::name)
                                .toList()
                )
                .minAge((short) 18)
                .maxAge((short) 60)
                .minIncome(0)
                .maxIncome(500000)
                .minMehr(0)
                .maxMehr(1000000)
                .build();
    }
}