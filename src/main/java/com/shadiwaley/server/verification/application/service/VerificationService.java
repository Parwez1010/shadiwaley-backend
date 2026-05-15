package com.shadiwaley.server.verification.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import com.shadiwaley.server.verification.domain.ReviewAction;
import com.shadiwaley.server.verification.dto.response.ReviewMediaResponse;
import com.shadiwaley.server.verification.dto.response.ReviewProfileDetailResponse;
import com.shadiwaley.server.verification.dto.response.ReviewQueueProfileResponse;
import com.shadiwaley.server.verification.infrastructure.entity.ProfileReviewLog;
import com.shadiwaley.server.verification.infrastructure.repository.ProfileReviewLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserProfileRepository userProfileRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProfileReviewLogRepository reviewLogRepository;

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    @Transactional(readOnly = true)
    public List<ReviewQueueProfileResponse> getProfilesForReview() {

        return userProfileRepository.findAll()
                .stream()
                .filter(profile ->
                        profile.getProfileStatus() == ProfileStatus.READY_FOR_REVIEW
                                || profile.getProfileStatus() == ProfileStatus.PENDING_VERIFICATION
                )
                .map(this::toReviewQueueResponse)
                .toList();
    }

    @Transactional
    public void approveProfile(UUID profileId, String note) {

        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        profile.setProfileStatus(ProfileStatus.LIVE);

        userProfileRepository.save(profile);

        auditLogService.record(
                AuditAction.PROFILE_APPROVED,
                AuditEntityType.USER_PROFILE,
                profile.getId(),
                "Profile approved by reviewer"
        );

        log(profile, ReviewAction.PROFILE_APPROVED, note);

        notificationService.create(
                profile.getUserAccount().getId(),
                NotificationType.PROFILE_APPROVED,
                "Profile approved",
                "Your profile has been approved. You are now ready for the next step.",
                "/profile",
                profile.getId()
        );
    }

    @Transactional
    public void rejectProfile(UUID profileId, String note) {

        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        profile.setProfileStatus(ProfileStatus.REJECTED);

        userProfileRepository.save(profile);
        auditLogService.record(
                AuditAction.PROFILE_REJECTED,
                AuditEntityType.USER_PROFILE,
                profile.getId(),
                "Profile rejected by reviewer"
        );

        log(profile, ReviewAction.PROFILE_REJECTED, note);

        notificationService.create(
                profile.getUserAccount().getId(),
                NotificationType.PROFILE_REJECTED,
                "Profile needs changes",
                "Your profile could not be approved yet. Please review the feedback and update the required details.",
                "/profile",
                profile.getId()
        );
    }

    @Transactional
    public void approveMedia(UUID mediaId, String note) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media not found"));

        media.setReviewStatus(MediaReviewStatus.APPROVED);

        mediaFileRepository.save(media);
        auditLogService.record(
                AuditAction.MEDIA_APPROVED,
                AuditEntityType.MEDIA_FILE,
                media.getId(),
                "Media approved by reviewer"
        );

        log(media.getUserProfile(), ReviewAction.MEDIA_APPROVED, note);

        notificationService.create(
                media.getUserAccount().getId(),
                NotificationType.MEDIA_APPROVED,
                "Media approved",
                "Your uploaded media has been reviewed and approved.",
                "/media",
                media.getId()
        );
    }

    @Transactional
    public void rejectMedia(UUID mediaId, String note) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media not found"));

        media.setReviewStatus(MediaReviewStatus.REJECTED);

        mediaFileRepository.save(media);
        auditLogService.record(
                AuditAction.MEDIA_REJECTED,
                AuditEntityType.MEDIA_FILE,
                media.getId(),
                "Media rejected by reviewer"
        );

        log(media.getUserProfile(), ReviewAction.MEDIA_REJECTED, note);

        notificationService.create(
                media.getUserAccount().getId(),
                NotificationType.MEDIA_REJECTED,
                "Media needs changes",
                "One of your uploaded files could not be approved. Please upload a clearer or correct file.",
                "/media",
                media.getId()
        );
    }

    private void log(
            UserProfile profile,
            ReviewAction action,
            String note
    ) {

        UUID reviewerId = AuthUser.getCurrentUserId();

        UserAccount reviewer = userAccountRepository.findById(reviewerId)
                .orElse(null);

        ProfileReviewLog log = new ProfileReviewLog();
        log.setUserProfile(profile);
        log.setReviewerUser(reviewer);
        log.setAction(action);
        log.setNote(note);

        reviewLogRepository.save(log);
    }
    private ReviewQueueProfileResponse  toReviewQueueResponse(UserProfile profile) {

        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(profile.getUserAccount().getId())
                .orElse(null);

        boolean hasProfilePhoto = mediaFileRepository
                .existsByUserProfileIdAndMediaTypeAndReviewStatusAndDeletedFalse(
                        profile.getId(),
                        MediaType.PROFILE_PHOTO,
                        MediaReviewStatus.APPROVED
                );

        boolean hasIdProof = mediaFileRepository
                .existsByUserProfileIdAndMediaTypeAndReviewStatusAndDeletedFalse(
                        profile.getId(),
                        MediaType.ID_PROOF,
                        MediaReviewStatus.APPROVED
                );

        boolean hasIncomeProof = mediaFileRepository
                .existsByUserProfileIdAndMediaTypeAndReviewStatusAndDeletedFalse(
                        profile.getId(),
                        MediaType.INCOME_PROOF,
                        MediaReviewStatus.APPROVED
                );
        boolean profilePhotoVerified = hasProfilePhoto;

        boolean idProofVerified = hasIdProof;

        boolean incomeProofVerified = hasIncomeProof;

        return ReviewQueueProfileResponse .builder()
                .profileId(profile.getId())
                .userId(profile.getUserAccount().getId())

                .candidateName(profile.getCandidateFirstName())
                .familyName(parent != null ? parent.getParentName() : null)
                .familyName(parent != null ? parent.getParentName() : null)
                .parentName(parent != null ? parent.getParentName() : null)
                .parentRelation(parent != null && parent.getParentRelation() != null
                        ? parent.getParentRelation().name()
                        : null)

                .phone(profile.getUserAccount().getPhone())

                .side(profile.getUserAccount().getSide().name())

                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .caste(parent != null ? parent.getCaste() : null)

                .phone(profile.getUserAccount().getPhone())

                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)

                .profileStatus(profile.getProfileStatus())
                .completionPct(profile.getCompletionPct())

                .hasProfilePhoto(hasProfilePhoto)
                .hasIdProof(hasIdProof)
                .hasIncomeProof(hasIncomeProof)

                .profilePhotoVerified(profilePhotoVerified)
                .idProofVerified(idProofVerified)
                .incomeProofVerified(incomeProofVerified)

                .createdAt(profile.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewProfileDetailResponse getReviewProfileDetail(UUID profileId) {

        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        UserAccount user = profile.getUserAccount();

        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(user.getId())
                .orElse(null);

        UserPreferences preferences = userPreferencesRepository
                .findByUserProfileId(profile.getId())
                .orElse(null);

        List<MediaFile> mediaFiles = mediaFileRepository
                .findByUserProfileIdAndDeletedFalseOrderByCreatedAtDesc(profile.getId());

        boolean hasProfilePhoto = hasMedia(mediaFiles, MediaType.PROFILE_PHOTO);
        boolean hasIdProof = hasMedia(mediaFiles, MediaType.ID_PROOF);
        boolean hasIncomeProof = hasMedia(mediaFiles, MediaType.INCOME_PROOF);

        boolean profilePhotoVerified = hasApprovedMedia(mediaFiles, MediaType.PROFILE_PHOTO);
        boolean idProofVerified = hasApprovedMedia(mediaFiles, MediaType.ID_PROOF);
        boolean incomeProofVerified = hasApprovedMedia(mediaFiles, MediaType.INCOME_PROOF);

        List<ReviewMediaResponse> mediaResponses = mediaFiles.stream()
                .map(this::toReviewMediaResponse)
                .toList();

        return ReviewProfileDetailResponse.builder()
                .profileId(profile.getId())
                .userId(user.getId())

                .candidateName(profile.getCandidateFirstName())
                .side(user.getSide() != null ? user.getSide().name() : null)
                .phone(user.getPhone())

                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus())

                .createdAt(profile.getCreatedAt())
                .submittedAt(profile.getUpdatedAt())

                .familyName(parent != null ? parent.getParentName() : null)
                .parentName(parent != null ? parent.getParentName() : null)
                .parentRelation(parent != null && parent.getParentRelation() != null
                        ? parent.getParentRelation().name()
                        : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .caste(parent != null ? parent.getCaste() : null)

                .age(profile.getCandidateAge())
                .heightCm(profile.getCandidateHeightCm())
                .education(profile.getEducation())
                .quranLevel(profile.getQuranLevel())
                .namaazRegularity(profile.getNamaazRegularity())
                .professionType(profile.getProfessionType())
                .professionTitle(profile.getProfessionTitle())
                .monthlyIncome(profile.getMonthlyIncome())
                .houseType(profile.getHouseType())
                .familyType(profile.getFamilyType())
                .expectationsText(profile.getExpectationsText())

                .preferredMaslak(preferences != null ? preferences.getPreferredMaslak() : null)
                .preferredCaste(preferences != null ? preferences.getPreferredCaste() : null)
                .preferredState(preferences != null ? preferences.getPreferredState() : null)
                .preferredDistrict(preferences != null ? preferences.getPreferredDistrict() : null)
                .minAge(preferences != null ? preferences.getMinAge() : null)
                .maxAge(preferences != null ? preferences.getMaxAge() : null)
                .preferredEducation(preferences != null ? preferences.getPreferredEducation() : null)
                .preferredFamilyType(preferences != null ? preferences.getPreferredFamilyType() : null)

                .hasProfilePhoto(hasProfilePhoto)
                .hasIdProof(hasIdProof)
                .hasIncomeProof(hasIncomeProof)

                .profilePhotoVerified(profilePhotoVerified)
                .idProofVerified(idProofVerified)
                .incomeProofVerified(incomeProofVerified)

                .imamRefVerified(false)
                .waliConsentRecorded(false)

                .media(mediaResponses)
                .build();
    }

    private boolean hasMedia(List<MediaFile> mediaFiles, MediaType mediaType) {
        return mediaFiles.stream()
                .anyMatch(media -> media.getMediaType() == mediaType);
    }

    private boolean hasApprovedMedia(List<MediaFile> mediaFiles, MediaType mediaType) {
        return mediaFiles.stream()
                .anyMatch(media ->
                        media.getMediaType() == mediaType
                                && media.getReviewStatus() == MediaReviewStatus.APPROVED
                );
    }

    private ReviewMediaResponse toReviewMediaResponse(MediaFile media) {
        return ReviewMediaResponse.builder()
                .mediaId(media.getId())
                .documentId(media.getId())
                .documentType(media.getMediaType())
                .fileName(media.getOriginalFileName())
                .adminPreviewUrl("/api/v1/admin/review/media/" + media.getId() + "/view")
                .verificationStatus(media.getReviewStatus())
                .rejectedReason(null)
                .uploadedAt(media.getCreatedAt())
                .build();
    }
}