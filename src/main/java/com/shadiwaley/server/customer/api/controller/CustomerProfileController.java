package com.shadiwaley.server.customer.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.customer.application.service.CustomerProfileService;
import com.shadiwaley.server.customer.dto.response.CustomerProfileResponse;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.onboarding.dto.response.OnboardingOptionsResponse;
import com.shadiwaley.server.onboarding.dto.response.OnboardingProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ApiResponse<CustomerProfileResponse> getProfile() {
        return ResponseFactory.success(
                "Customer profile fetched successfully",
                customerProfileService.getProfile()
        );
    }

    @PatchMapping
    public ApiResponse<OnboardingProfileResponse> updateProfile(
            @Valid @RequestBody OnboardingProfileUpsertRequest request
    ) {
        return ResponseFactory.success(
                "Customer profile updated successfully",
                customerProfileService.updateProfile(request)
        );
    }

    @GetMapping("/completion")
    public ApiResponse<OnboardingProfileResponse> completion() {
        return ResponseFactory.success(
                "Customer profile completion fetched successfully",
                customerProfileService.getCompletion()
        );
    }

    @GetMapping("/options")
    public ApiResponse<OnboardingOptionsResponse> options() {
        return ResponseFactory.success(
                "Customer profile options fetched successfully",
                customerProfileService.getOptions()
        );
    }
}