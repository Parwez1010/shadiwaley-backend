package com.shadiwaley.server.customer.application.service;

import com.shadiwaley.server.customer.dto.response.CustomerProfileResponse;
import com.shadiwaley.server.onboarding.application.service.OnboardingOptionsService;
import com.shadiwaley.server.onboarding.application.service.OnboardingService;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.onboarding.dto.response.OnboardingOptionsResponse;
import com.shadiwaley.server.onboarding.dto.response.OnboardingProfileResponse;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    private final OnboardingService onboardingService;
    private final OnboardingOptionsService onboardingOptionsService;

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile() {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        ParentProfile parent = parentProfileRepository.findByUserAccountId(userId)
                .orElse(null);

        UserPreferences preferences = userPreferencesRepository.findByUserProfileId(profile.getId())
                .orElse(null);

        OnboardingProfileResponse completion = onboardingService.getCompletion();

        return CustomerProfileResponse.builder()
                .userId(user.getId())
                .profileId(profile.getId())
                .phone(user.getPhone())
                .side(user.getSide() != null ? user.getSide().name() : null)
                .profileStatus(profile.getProfileStatus())
                .completionPct(profile.getCompletionPct())
                .readyForReview(completion.isReadyForReview())
                .missingFields(completion.getMissingFields())
                .parent(toParentInfo(parent))
                .profile(toProfileInfo(profile))
                .preferences(toPreferenceInfo(preferences))
                .build();
    }

    @Transactional
    public OnboardingProfileResponse updateProfile(OnboardingProfileUpsertRequest request) {
        return onboardingService.upsertProfile(request);
    }

    @Transactional(readOnly = true)
    public OnboardingProfileResponse getCompletion() {
        return onboardingService.getCompletion();
    }

    @Transactional(readOnly = true)
    public OnboardingOptionsResponse getOptions() {
        return onboardingOptionsService.getOptions();
    }

    private CustomerProfileResponse.ParentInfo toParentInfo(ParentProfile parent) {
        if (parent == null) {
            return null;
        }

        return CustomerProfileResponse.ParentInfo.builder()
                .parentName(parent.getParentName())
                .parentRelation(parent.getParentRelation())
                .parentPhone(parent.getParentPhone())
                .district(parent.getDistrict())
                .state(parent.getState())
                .maslak(parent.getMaslak())
                .imamReference(parent.getImamReference())
                .masjidName(parent.getMasjidName())
                .caste(parent.getCaste())
                .build();
    }

    private CustomerProfileResponse.ProfileInfo toProfileInfo(UserProfile profile) {
        return CustomerProfileResponse.ProfileInfo.builder()
                .candidateFirstName(profile.getCandidateFirstName())
                .candidateAge(profile.getCandidateAge())
                .candidateHeightCm(profile.getCandidateHeightCm())
                .education(profile.getEducation())
                .religion(profile.getReligion())
                .maritalStatus(profile.getMaritalStatus())
                .quranLevel(profile.getQuranLevel())
                .namaazRegularity(profile.getNamaazRegularity())
                .previouslyMarried(profile.getPreviouslyMarried())
                .professionType(profile.getProfessionType())
                .professionTitle(profile.getProfessionTitle())
                .monthlyIncome(profile.getMonthlyIncome())
                .mehrOffered(profile.getMehrOffered())
                .mehrMinimumExpected(profile.getMehrMinimumExpected())
                .houseType(profile.getHouseType())
                .familyType(profile.getFamilyType())
                .expectationsText(profile.getExpectationsText())
                // NEW
                .dateOfBirth(profile.getDateOfBirth())
                .bloodGroup(profile.getBloodGroup())
                .complexion(profile.getComplexion())
                .bodyType(profile.getBodyType())
                .motherTongue(profile.getMotherTongue())
                .languagesKnown(profile.getLanguagesKnown())
                .diet(profile.getDiet())
                .smoker(profile.getSmoker())
                .drinker(profile.getDrinker())
                .exerciseFrequency(profile.getExerciseFrequency())
                .wearsHijab(profile.getWearsHijab())
                .familyStatus(profile.getFamilyStatus())
                .familyValues(profile.getFamilyValues())
                .brothersCount(profile.getBrothersCount())
                .sistersCount(profile.getSistersCount())
                .interests(profile.getInterests())
                .build();
    }


    private CustomerProfileResponse.PreferenceInfo toPreferenceInfo(UserPreferences preferences) {
        if (preferences == null) {
            return null;
        }

        return CustomerProfileResponse.PreferenceInfo.builder()
                .preferredMaslak(preferences.getPreferredMaslak())
                .preferredState(preferences.getPreferredState())
                .preferredDistrict(preferences.getPreferredDistrict())
                .preferredCaste(preferences.getPreferredCaste())
                .minAge(preferences.getMinAge())
                .maxAge(preferences.getMaxAge())
                .preferredEducation(preferences.getPreferredEducation())
                .preferredFamilyType(preferences.getPreferredFamilyType())
                .requireImamRef(preferences.isRequireImamRef())
                .requireIdVerified(preferences.isRequireIdVerified())
                .build();
    }
}