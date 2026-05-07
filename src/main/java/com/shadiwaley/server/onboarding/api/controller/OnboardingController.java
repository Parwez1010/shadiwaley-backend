package com.shadiwaley.server.onboarding.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.onboarding.application.service.OnboardingService;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.onboarding.dto.response.OnboardingProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PatchMapping("/profile")
    public ApiResponse<OnboardingProfileResponse> upsertProfile(
            @RequestBody OnboardingProfileUpsertRequest request
    ) {
        return ResponseFactory.success("Profile saved successfully", onboardingService.upsertProfile(request));
    }

    @GetMapping("/profile/completion")
    public ApiResponse<OnboardingProfileResponse> getCompletion() {
        return ResponseFactory.success("Profile completion fetched successfully", onboardingService.getCompletion());
    }
}