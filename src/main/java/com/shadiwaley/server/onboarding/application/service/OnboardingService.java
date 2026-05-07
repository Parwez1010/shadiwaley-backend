package com.shadiwaley.server.onboarding.application.service;

import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.onboarding.dto.request.ParentInfoRequest;
import com.shadiwaley.server.onboarding.dto.request.PreferenceInfoRequest;
import com.shadiwaley.server.onboarding.dto.request.ProfileInfoRequest;
import com.shadiwaley.server.onboarding.dto.response.MissingFieldResponse;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;

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

        List<MissingFieldResponse> missingFields = calculateMissingFields(profile, parent, userAccount);
        short completionPct = calculateCompletion(profile, parent);

        profile.setCompletionPct(completionPct);

        if (completionPct >= 80 && "INCOMPLETE".equals(profile.getProfileStatus())) {
            profile.setProfileStatus("PENDING_VERIFICATION");
        }

        parentProfileRepository.save(parent);
        userProfileRepository.save(profile);
        userPreferencesRepository.save(preferences);

        return OnboardingProfileResponse.builder()
                .profileId(profile.getId())
                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus())
                .missingFields(missingFields)
                .build();
    }

    public OnboardingProfileResponse getCompletion() {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        ParentProfile parent = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        List<MissingFieldResponse> missingFields = calculateMissingFields(profile, parent, userAccount);

        return OnboardingProfileResponse.builder()
                .profileId(profile.getId())
                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus())
                .missingFields(missingFields)
                .build();
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

    private short calculateCompletion(UserProfile profile, ParentProfile parent) {
        int score = 0;

        if (hasText(parent.getParentName())
                && parent.getParentRelation() != null
                && hasText(parent.getParentPhone())
                && hasText(parent.getDistrict())
                && hasText(parent.getState())
                && hasText(parent.getMaslak())) {
            score += 30;
        }

        if (hasText(profile.getCandidateFirstName())
                && profile.getCandidateAge() != null
                && profile.getCandidateHeightCm() != null
                && hasText(profile.getEducation())) {
            score += 25;
        }

        if (hasText(profile.getQuranLevel())
                && hasText(profile.getNamaazRegularity())
                && hasText(parent.getImamReference())) {
            score += 20;
        }

        if (hasText(profile.getFamilyType())
                && hasText(profile.getExpectationsText())) {
            score += 15;
        }

        if (("BOY".equals(profile.getUserAccount().getSide().name()) && profile.getMehrOffered() != null)
                || ("GIRL".equals(profile.getUserAccount().getSide().name()) && profile.getMehrMinimumExpected() != null)) {
            score += 10;
        }

        return (short) Math.min(score, 100);
    }

    private List<MissingFieldResponse> calculateMissingFields(
            UserProfile profile,
            ParentProfile parent,
            UserAccount userAccount
    ) {
        List<MissingFieldResponse> missing = new ArrayList<>();

        if (!hasText(parent.getParentName())) {
            missing.add(missing("PARENT_NAME", "Parent name add karein", "HIGH"));
        }

        if (parent.getParentRelation() == null) {
            missing.add(missing("PARENT_RELATION", "Parent relation select karein", "HIGH"));
        }

        if (!hasText(parent.getDistrict())) {
            missing.add(missing("DISTRICT", "District add karein", "HIGH"));
        }

        if (!hasText(parent.getMaslak())) {
            missing.add(missing("MASLAK", "Maslak select karein", "HIGH"));
        }

        if (!hasText(profile.getCandidateFirstName())) {
            missing.add(missing("CANDIDATE_NAME", "Candidate name add karein", "HIGH"));
        }

        if (profile.getCandidateAge() == null) {
            missing.add(missing("CANDIDATE_AGE", "Candidate age add karein", "HIGH"));
        }

        if (!hasText(profile.getEducation())) {
            missing.add(missing("EDUCATION", "Education add karein", "MEDIUM"));
        }

        if (!hasText(profile.getQuranLevel())) {
            missing.add(missing("QURAN_LEVEL", "Quran level add karein", "MEDIUM"));
        }

        if (!hasText(profile.getNamaazRegularity())) {
            missing.add(missing("NAMAAZ_REGULARITY", "Namaaz regularity add karein", "MEDIUM"));
        }

        if (!hasText(parent.getImamReference())) {
            missing.add(missing("IMAM_REFERENCE", "Imam reference add karein", "HIGH"));
        }

        if ("BOY".equals(userAccount.getSide().name()) && profile.getMehrOffered() == null) {
            missing.add(missing("MEHR_OFFERED", "Mehr offered add karein", "HIGH"));
        }

        if ("GIRL".equals(userAccount.getSide().name()) && profile.getMehrMinimumExpected() == null) {
            missing.add(missing("MEHR_EXPECTED", "Mehr expected add karein", "HIGH"));
        }

        return missing;
    }

    private MissingFieldResponse missing(String field, String label, String priority) {
        return MissingFieldResponse.builder()
                .field(field)
                .label(label)
                .priority(priority)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}