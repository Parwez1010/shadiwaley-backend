package com.shadiwaley.server.publicbrowse.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.profile.domain.MaritalStatus;
import com.shadiwaley.server.profile.dto.response.ProfileDetailResponse;
import com.shadiwaley.server.publicbrowse.application.service.PublicBrowseService;
import com.shadiwaley.server.publicbrowse.dto.response.PublicBrowseProfilePageResponse;
import com.shadiwaley.server.customer.browse.dto.response.CustomerBrowseFiltersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/browse")
@RequiredArgsConstructor
public class PublicBrowseController {

    private final PublicBrowseService publicBrowseService;

    @GetMapping("/profiles")
    public ApiResponse<PublicBrowseProfilePageResponse> browseProfiles(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
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
            @RequestParam(required = false) Integer minMehr,
            @RequestParam(required = false) Integer maxMehr
    ) {
        return ResponseFactory.success(
                "Public profiles fetched successfully",
                publicBrowseService.browseProfiles(
                        page,
                        size,
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
                "Public profile fetched successfully",
                publicBrowseService.getProfileDetail(profileId)
        );
    }

    @GetMapping("/filters")
    public ApiResponse<CustomerBrowseFiltersResponse> getFilters() {
        return ResponseFactory.success(
                "Public browse filters fetched successfully",
                publicBrowseService.getFilters()
        );
    }
}