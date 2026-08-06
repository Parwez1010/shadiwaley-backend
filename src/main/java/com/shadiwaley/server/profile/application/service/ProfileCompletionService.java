package com.shadiwaley.server.profile.application.service;

import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.dto.response.MissingFieldResponse;
import com.shadiwaley.server.profile.dto.response.ProfileCompletionResponse;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileCompletionService {

    private final MediaFileRepository mediaFileRepository;
    private final ConsequenceCopyService consequenceCopyService;

    public ProfileCompletionResponse recalculateAndApply(
            UserAccount userAccount,
            UserProfile profile,
            ParentProfile parent
    ) {
        List<MissingFieldResponse> missingFields = calculateMissingFields(userAccount, profile, parent);
        short completionPct = calculateCompletion(userAccount, profile, parent);

        profile.setCompletionPct(completionPct);
        profile.setProfileStatus(resolveProfileStatus(profile, missingFields));

        return buildResponse(profile, missingFields);
    }

    public ProfileCompletionResponse recalculateAndApplyForCustomerOnboarding(
            UserAccount userAccount,
            UserProfile profile,
            ParentProfile parent
    ) {
        List<MissingFieldResponse> missingFields =
                calculateMissingFields(userAccount, profile, parent);

        short completionPct =
                calculateCompletion(userAccount, profile, parent);

        profile.setCompletionPct(completionPct);
        profile.setProfileStatus(resolveCustomerOnboardingStatus(profile, missingFields));

        return buildResponse(profile, missingFields);
    }


    public ProfileCompletionResponse buildCurrentResponse(
            UserAccount userAccount,
            UserProfile profile,
            ParentProfile parent
    ) {
        List<MissingFieldResponse> missingFields = calculateMissingFields(userAccount, profile, parent);
        return buildResponse(profile, missingFields);
    }

    private ProfileStatus resolveProfileStatus(
            UserProfile profile,
            List<MissingFieldResponse> missingFields
    ) {
        if (profile.getProfileStatus() == ProfileStatus.VERIFIED
                || profile.getProfileStatus() == ProfileStatus.LIVE
                || profile.getProfileStatus() == ProfileStatus.SUSPENDED) {
            return profile.getProfileStatus();
        }

        boolean hasHighMissing = missingFields.stream()
                .anyMatch(field -> "HIGH".equals(field.getPriority()));

        if (profile.getCompletionPct() >= 85 && !hasHighMissing) {
            return ProfileStatus.READY_FOR_REVIEW;
        }

        if (profile.getCompletionPct() >= 70) {
            return ProfileStatus.PENDING_VERIFICATION;
        }

        return ProfileStatus.INCOMPLETE;
    }


    private short calculateCompletion(
            UserAccount userAccount,
            UserProfile profile,
            ParentProfile parent
    ) {
        int score = 0;

        if (hasText(parent.getParentName())
                && parent.getParentRelation() != null
                && hasText(parent.getParentPhone())
                && hasText(parent.getDistrict())
                && hasText(parent.getState())
                && hasText(parent.getMaslak())) {
            score += 20;
        }

        if (hasText(profile.getCandidateFirstName())
                && profile.getCandidateAge() != null
                && profile.getCandidateHeightCm() != null
                && hasText(profile.getEducation())) {
            score += 15;
        }

        // NEW — extended basic info
        if (hasText(profile.getBloodGroup())
                && hasText(profile.getComplexion())
                && profile.getBodyType() != null
                && hasText(profile.getMotherTongue())
                && hasItems(profile.getLanguagesKnown())) {
            score += 10;
        }

        if (hasText(profile.getQuranLevel())
                && hasText(profile.getNamaazRegularity())
                && hasText(parent.getImamReference())) {
            score += 15;
        }

        // NEW — lifestyle
        boolean lifestyleComplete = profile.getDiet() != null
                && profile.getSmoker() != null
                && profile.getDrinker() != null
                && profile.getExerciseFrequency() != null
                && (isBoy(userAccount) || profile.getWearsHijab() != null);

        if (lifestyleComplete) {
            score += 10;
        }

        // family + interests
        if (hasText(profile.getFamilyType())
                && hasText(profile.getExpectationsText())
                && profile.getFamilyStatus() != null
                && profile.getFamilyValues() != null) {
            score += 10;
        }

        if (isBoy(userAccount) && profile.getMehrOffered() != null) {
            score += 10;
        }

        if (isGirl(userAccount) && profile.getMehrMinimumExpected() != null) {
            score += 10;
        }

        if (hasProfilePhoto(profile)) {
            score += 10;
        }

        if (hasIdProof(profile)) {
            score += 10;
        }

        if (isBoy(userAccount) && hasIncomeProof(profile)) {
            score += 10;
        }

        return (short) Math.min(score, 100);
    }


    private List<MissingFieldResponse> calculateMissingFields(
            UserAccount userAccount,
            UserProfile profile,
            ParentProfile parent
    ) {
        List<MissingFieldResponse> missing = new ArrayList<>();

        if (!hasText(parent.getParentName())) {
            missing.add(missing("PARENT_NAME", "Add parent name", "HIGH"));
        }
        if (parent.getParentRelation() == null) {
            missing.add(missing("PARENT_RELATION", "Select parent relation", "HIGH"));
        }
        if (!hasText(parent.getDistrict())) {
            missing.add(missing("DISTRICT", "Add district", "HIGH"));
        }
        if (!hasText(parent.getMaslak())) {
            missing.add(missing("MASLAK", "Select Maslak", "HIGH"));
        }
        if (!hasText(profile.getCandidateFirstName())) {
            missing.add(missing("CANDIDATE_NAME", "Add candidate name", "HIGH"));
        }
        if (profile.getCandidateAge() == null) {
            missing.add(missing("CANDIDATE_AGE", "Add candidate age", "HIGH"));
        }
        if (!hasText(profile.getEducation())) {
            missing.add(missing("EDUCATION", "Add education details", "MEDIUM"));
        }

        // NEW — extended basic info
        if (!hasText(profile.getMotherTongue())) {
            missing.add(missing("MOTHER_TONGUE", "Add mother tongue", "MEDIUM"));
        }
        if (!hasText(profile.getComplexion())) {
            missing.add(missing("COMPLEXION", "Add complexion", "LOW"));
        }
        if (profile.getBodyType() == null) {
            missing.add(missing("BODY_TYPE", "Add body type", "LOW"));
        }
        if (!hasItems(profile.getLanguagesKnown())) {
            missing.add(missing("LANGUAGES_KNOWN", "Add languages known", "LOW"));
        }

        if (!hasText(profile.getQuranLevel())) {
            missing.add(missing("QURAN_LEVEL", "Add Quran learning details", "MEDIUM"));
        }
        if (!hasText(profile.getNamaazRegularity())) {
            missing.add(missing("NAMAAZ_REGULARITY", "Add prayer regularity details", "MEDIUM"));
        }
        if (!hasText(parent.getImamReference())) {
            missing.add(missing("IMAM_REFERENCE", "Add Imam reference", "HIGH"));
        }

        // NEW — lifestyle
        if (profile.getDiet() == null) {
            missing.add(missing("DIET", "Add dietary preference", "MEDIUM"));
        }
        if (profile.getSmoker() == null || profile.getDrinker() == null) {
            missing.add(missing("SMOKING_DRINKING", "Add smoking/drinking habits", "MEDIUM"));
        }
        if (isGirl(userAccount) && profile.getWearsHijab() == null) {
            missing.add(missing("WEARS_HIJAB", "Add hijab preference", "MEDIUM"));
        }

        // NEW — family
        if (profile.getFamilyStatus() == null) {
            missing.add(missing("FAMILY_STATUS", "Add family status", "MEDIUM"));
        }
        if (profile.getFamilyValues() == null) {
            missing.add(missing("FAMILY_VALUES", "Add family values", "MEDIUM"));
        }
        if (!hasItems(profile.getInterests())) {
            missing.add(missing("INTERESTS", "Add a few interests", "LOW"));
        }

        if (isBoy(userAccount) && profile.getMehrOffered() == null) {
            missing.add(missing("MEHR_OFFERED", "Add Mehr offered", "HIGH"));
        }
        if (isGirl(userAccount) && profile.getMehrMinimumExpected() == null) {
            missing.add(missing("MEHR_EXPECTED", "Add expected Mehr", "HIGH"));
        }
        if (!hasProfilePhoto(profile)) {
            missing.add(missing("PROFILE_PHOTO", "Upload candidate photo", "HIGH"));
        }
        if (!hasIdProof(profile)) {
            missing.add(missing("ID_PROOF", "Upload ID proof", "HIGH"));
        }
        if (isBoy(userAccount) && !hasIncomeProof(profile)) {
            missing.add(missing("INCOME_PROOF", "Upload income proof", "MEDIUM"));
        }

        return missing;
    }

    private boolean hasItems(java.util.Set<String> values) {
        return values != null && !values.isEmpty();
    }


    private MissingFieldResponse missing(String field, String label, String priority) {
        return MissingFieldResponse.builder()
                .field(field)
                .label(label)
                .priority(priority)
                .consequenceMessage(consequenceCopyService.getMessage(field))
                .build();
    }

    private ProfileCompletionResponse buildResponse(
            UserProfile profile,
            List<MissingFieldResponse> missingFields
    ) {
        return ProfileCompletionResponse.builder()
                .profileId(profile.getId())
                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus().name())
                .readyForReview(profile.getProfileStatus() == ProfileStatus.READY_FOR_REVIEW)
                .missingFields(missingFields)
                .build();
    }

    private boolean hasProfilePhoto(UserProfile profile) {

        return mediaFileRepository
                .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                        profile.getId(),
                        MediaType.PROFILE_PHOTO
                )
                .stream()
                .anyMatch(media ->
                        media.getReviewStatus() == com.shadiwaley.server.media.domain.MediaReviewStatus.APPROVED
                );
    }

    private boolean hasIdProof(UserProfile profile) {
        return !mediaFileRepository
                .findByUserAccountIdAndMediaTypeAndDeletedFalse(
                        profile.getUserAccount().getId(),
                        MediaType.ID_PROOF
                )
                .isEmpty();
    }

    private boolean hasIncomeProof(UserProfile profile) {
        return !mediaFileRepository
                .findByUserAccountIdAndMediaTypeAndDeletedFalse(
                        profile.getUserAccount().getId(),
                        MediaType.INCOME_PROOF
                )
                .isEmpty();
    }

    private boolean isBoy(UserAccount userAccount) {
        return "BOY".equals(userAccount.getSide().name());
    }

    private boolean isGirl(UserAccount userAccount) {
        return "GIRL".equals(userAccount.getSide().name());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private ProfileStatus resolveCustomerOnboardingStatus(
            UserProfile profile,
            List<MissingFieldResponse> missingFields
    ) {
        if (profile.getProfileStatus() == ProfileStatus.SUSPENDED) {
            return ProfileStatus.SUSPENDED;
        }

        boolean hasHighMissing = missingFields.stream()
                .anyMatch(field -> "HIGH".equals(field.getPriority()));

        /*
         * Customer onboarding must never make profile LIVE directly.
         * LIVE should only happen after admin/verifier approval.
         */
        if (profile.getCompletionPct() >= 85 && !hasHighMissing) {
            return ProfileStatus.PENDING_VERIFICATION;
        }

        if (profile.getCompletionPct() >= 70) {
            return ProfileStatus.PENDING_VERIFICATION;
        }

        return ProfileStatus.INCOMPLETE;
    }
}