package com.shadiwaley.server.onboarding.application.service;

import com.shadiwaley.server.engagement.application.service.MilestoneService;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.onboarding.dto.request.ParentInfoRequest;
import com.shadiwaley.server.onboarding.dto.request.PreferenceInfoRequest;
import com.shadiwaley.server.onboarding.dto.request.ProfileInfoRequest;
import com.shadiwaley.server.onboarding.dto.response.OnboardingProfileResponse;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.application.service.ProfileCompletionService;
import com.shadiwaley.server.profile.dto.response.ProfileCompletionResponse;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ProfileCompletionService profileCompletionService;
    private final MilestoneService milestoneService;

    @Transactional
    public OnboardingProfileResponse upsertProfile(OnboardingProfileUpsertRequest request) {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        ParentProfile parent = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        UserPreferences preferences = userPreferencesRepository.findByUserProfileId(profile.getId())
                .orElseThrow(() -> new EntityNotFoundException("User preferences not found"));

        if (request.getParent() != null) {
            updateParent(parent, request.getParent());
        }

        if (request.getProfile() != null) {
            updateProfile(profile, request.getProfile());
        }

        if (request.getPreferences() != null) {
            updatePreferences(preferences, request.getPreferences());
        }

        ProfileCompletionResponse completion =
                profileCompletionService.recalculateAndApply(userAccount, profile, parent);

        milestoneService.evaluateMilestones(userId);

        parentProfileRepository.save(parent);
        userProfileRepository.save(profile);
        userPreferencesRepository.save(preferences);

        return toOnboardingResponse(completion);
    }

    public OnboardingProfileResponse getCompletion() {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        ParentProfile parent = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        ProfileCompletionResponse completion =
                profileCompletionService.buildCurrentResponse(userAccount, profile, parent);

        return toOnboardingResponse(completion);
    }

    private void updateParent(ParentProfile parent, ParentInfoRequest request) {
        if (request.getParentName() != null) parent.setParentName(request.getParentName());
        if (request.getParentRelation() != null) parent.setParentRelation(request.getParentRelation());
        if (request.getParentPhone() != null) parent.setParentPhone(request.getParentPhone());
        if (request.getDistrict() != null) parent.setDistrict(request.getDistrict());
        if (request.getState() != null) parent.setState(request.getState());
        if (request.getMaslak() != null) parent.setMaslak(request.getMaslak());
        if (request.getImamReference() != null) parent.setImamReference(request.getImamReference());
        if (request.getMasjidName() != null) parent.setMasjidName(request.getMasjidName());
    }

    private void updateProfile(UserProfile profile, ProfileInfoRequest request) {
        if (request.getCandidateFirstName() != null) profile.setCandidateFirstName(request.getCandidateFirstName());
        if (request.getCandidateAge() != null) profile.setCandidateAge(request.getCandidateAge());
        if (request.getCandidateHeightCm() != null) profile.setCandidateHeightCm(request.getCandidateHeightCm());
        if (request.getEducation() != null) profile.setEducation(request.getEducation());
        if (request.getQuranLevel() != null) profile.setQuranLevel(request.getQuranLevel());
        if (request.getNamaazRegularity() != null) profile.setNamaazRegularity(request.getNamaazRegularity());
        if (request.getPreviouslyMarried() != null) profile.setPreviouslyMarried(request.getPreviouslyMarried());
        if (request.getProfessionType() != null) profile.setProfessionType(request.getProfessionType());
        if (request.getProfessionTitle() != null) profile.setProfessionTitle(request.getProfessionTitle());
        if (request.getMonthlyIncome() != null) profile.setMonthlyIncome(request.getMonthlyIncome());
        if (request.getMehrOffered() != null) profile.setMehrOffered(request.getMehrOffered());
        if (request.getMehrMinimumExpected() != null) profile.setMehrMinimumExpected(request.getMehrMinimumExpected());
        if (request.getHouseType() != null) profile.setHouseType(request.getHouseType());
        if (request.getFamilyType() != null) profile.setFamilyType(request.getFamilyType());
        if (request.getExpectationsText() != null) profile.setExpectationsText(request.getExpectationsText());
    }

    private void updatePreferences(UserPreferences preferences, PreferenceInfoRequest request) {
        if (request.getPreferredMaslak() != null) preferences.setPreferredMaslak(request.getPreferredMaslak());
        if (request.getPreferredState() != null) preferences.setPreferredState(request.getPreferredState());
        if (request.getPreferredDistrict() != null) preferences.setPreferredDistrict(request.getPreferredDistrict());
        if (request.getMinAge() != null) preferences.setMinAge(request.getMinAge());
        if (request.getMaxAge() != null) preferences.setMaxAge(request.getMaxAge());
        if (request.getPreferredEducation() != null) preferences.setPreferredEducation(request.getPreferredEducation());
        if (request.getPreferredFamilyType() != null) preferences.setPreferredFamilyType(request.getPreferredFamilyType());
        if (request.getRequireImamRef() != null) preferences.setRequireImamRef(request.getRequireImamRef());
        if (request.getRequireIdVerified() != null) preferences.setRequireIdVerified(request.getRequireIdVerified());
    }

    private OnboardingProfileResponse toOnboardingResponse(ProfileCompletionResponse completion) {
        return OnboardingProfileResponse.builder()
                .profileId(completion.getProfileId())
                .completionPct(completion.getCompletionPct())
                .profileStatus(completion.getProfileStatus())
                .readyForReview(completion.isReadyForReview())
                .missingFields(completion.getMissingFields())
                .build();
    }
}