package com.shadiwaley.server.customer.browse.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.customer.browse.application.service.CustomerBrowseService;
import com.shadiwaley.server.customer.browse.dto.response.CustomerBrowseFiltersResponse;
import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.profile.dto.response.BrowseProfilesResponse;
import com.shadiwaley.server.profile.dto.response.ProfileDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/browse")
@RequiredArgsConstructor
public class CustomerBrowseController {

    private final CustomerBrowseService customerBrowseService;

    @GetMapping("/profiles")
    public ApiResponse<BrowseProfilesResponse> browseProfiles(
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
        return ResponseFactory.success(
                "Profiles fetched successfully",
                customerBrowseService.browseProfiles(
                        page,
                        size,
                        country,
                        state,
                        district,
                        maslak,
                        minAge,
                        maxAge,
                        education,
                        familyType,
                        professionType,
                        quranLevel,
                        namaazRegularity,
                        maritalStatus,
                        minIncome,
                        maxIncome,
                        minMehr,
                        maxMehr
                )
        );
    }

    @GetMapping("/profiles/{profileId}")
    public ApiResponse<ProfileDetailResponse> getProfileDetail(
            @PathVariable UUID profileId
    ) {
        return ResponseFactory.success(
                "Profile fetched successfully",
                customerBrowseService.getProfileDetail(profileId)
        );
    }

    @GetMapping("/filters")
    public ApiResponse<CustomerBrowseFiltersResponse> getFilters() {
        return ResponseFactory.success(
                "Browse filters fetched successfully",
                customerBrowseService.getFilters()
        );
    }

}