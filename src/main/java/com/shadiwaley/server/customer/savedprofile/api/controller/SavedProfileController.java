package com.shadiwaley.server.customer.savedprofile.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.customer.savedprofile.application.service.SavedProfileService;
import com.shadiwaley.server.profile.dto.response.ProfileCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/saved-profiles")
@RequiredArgsConstructor
public class SavedProfileController {

    private final SavedProfileService savedProfileService;

    @PostMapping("/{profileId}")
    public ApiResponse<Void> saveProfile(
            @PathVariable UUID profileId
    ) {
        savedProfileService.saveProfile(profileId);

        return ResponseFactory.success(
                "Profile saved successfully",
                null
        );
    }

    @DeleteMapping("/{profileId}")
    public ApiResponse<Void> removeSavedProfile(
            @PathVariable UUID profileId
    ) {
        savedProfileService.removeSavedProfile(profileId);

        return ResponseFactory.success(
                "Saved profile removed successfully",
                null
        );
    }

    @GetMapping
    public ApiResponse<List<ProfileCardResponse>> getSavedProfiles() {
        return ResponseFactory.success(
                "Saved profiles fetched successfully",
                savedProfileService.getSavedProfiles()
        );
    }
}