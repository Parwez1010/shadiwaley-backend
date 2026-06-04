package com.shadiwaley.server.onboarding.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.onboarding.application.service.OnboardingOptionsService;
import com.shadiwaley.server.onboarding.dto.response.OnboardingOptionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingOptionsController {

    private final OnboardingOptionsService onboardingOptionsService;

    @GetMapping("/options")
    public ApiResponse<OnboardingOptionsResponse> getOptions() {
        return ResponseFactory.success(
                "Onboarding options fetched successfully",
                onboardingOptionsService.getOptions()
        );
    }
}