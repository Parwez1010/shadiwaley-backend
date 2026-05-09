package com.shadiwaley.server.profile.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.profile.application.service.ProfileBrowseService;
import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.profile.dto.request.BrowseProfileFilterRequest;
import com.shadiwaley.server.profile.dto.response.BrowseProfilesResponse;
import com.shadiwaley.server.profile.dto.response.ProfileDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileBrowseController {

    private final ProfileBrowseService profileBrowseService;

    @GetMapping("/browse")
    public ApiResponse<BrowseProfilesResponse> browse(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String maslak,
            @RequestParam(required = false) Short minAge,
            @RequestParam(required = false) Short maxAge,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String familyType,
            @RequestParam(required = false) String professionType,
            @RequestParam(required = false) String quranLevel,
            @RequestParam(required = false) String namaazRegularity,
            @RequestParam(required = false) MaritalStatus maritalStatus,
            @RequestParam(required = false) Integer minIncome,
            @RequestParam(required = false) Integer maxIncome,
            @RequestParam(required = false) Integer minMehr,
            @RequestParam(required = false) Integer maxMehr
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

        return ResponseFactory.success(
                "Profiles fetched successfully",
                profileBrowseService.browse(filter)
        );
    }

    @GetMapping("/{profileId}")
    public ApiResponse<ProfileDetailResponse> getProfileDetail(@PathVariable UUID profileId) {
        return ResponseFactory.success(
                "Profile fetched successfully",
                profileBrowseService.getProfileDetail(profileId)
        );
    }
}