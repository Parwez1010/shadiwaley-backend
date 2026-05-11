package com.shadiwaley.server.admin.dto.request;

import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.user.domain.UserSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdminFamilyRequest {

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotNull(message = "Side is required")
    private UserSide side;

    @Valid
    @NotNull(message = "Onboarding data is required")
    private OnboardingProfileUpsertRequest onboarding;
}