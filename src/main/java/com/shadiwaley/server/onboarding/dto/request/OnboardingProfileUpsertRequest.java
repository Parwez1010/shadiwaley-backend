package com.shadiwaley.server.onboarding.dto.request;

import com.shadiwaley.server.revenue.dto.request.OnboardingPlanRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingProfileUpsertRequest {
    private ParentInfoRequest parent;
    private ProfileInfoRequest profile;
    private PreferenceInfoRequest preferences;
    private OnboardingPlanRequest plan;
}