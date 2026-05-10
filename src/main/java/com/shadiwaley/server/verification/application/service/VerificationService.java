package com.shadiwaley.server.verification.application.service;

import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import com.shadiwaley.server.verification.domain.ReviewAction;
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

    private final com.shadiwaley.server.notification.application.service.NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ReviewQueueProfileResponse> getProfilesForReview() {

        return userProfileRepository.findAll()
                .stream()
                .filter(profile ->
                        profile.getProfileStatus() == ProfileStatus.READY_FOR_REVIEW
                                || profile.getProfileStatus() == ProfileStatus.PENDING_VERIFICATION
                )
                .map(this::toQueueResponse)
                .toList();
    }

    @Transactional
    public void approveProfile(UUID profileId, String note) {

        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        profile.setProfileStatus(ProfileStatus.LIVE);

        userProfileRepository.save(profile);

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

    private ReviewQueueProfileResponse toQueueResponse(UserProfile profile) {

        UUID profileId = profile.getId();

        boolean hasProfilePhoto =
                mediaFileRepository
                        .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                                profileId,
                                MediaType.PROFILE_PHOTO
                        )
                        .isPresent();

        boolean hasIdProof =
                !mediaFileRepository
                        .findByUserAccountIdAndMediaTypeAndDeletedFalse(
                                profile.getUserAccount().getId(),
                                MediaType.ID_PROOF
                        )
                        .isEmpty();

        boolean hasIncomeProof =
                !mediaFileRepository
                        .findByUserAccountIdAndMediaTypeAndDeletedFalse(
                                profile.getUserAccount().getId(),
                                MediaType.INCOME_PROOF
                        )
                        .isEmpty();

        return ReviewQueueProfileResponse.builder()
                .profileId(profile.getId())
                .userId(profile.getUserAccount().getId())
                .candidateName(profile.getCandidateFirstName())
                .phone(profile.getUserAccount().getPhone())
                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus())
                .hasProfilePhoto(hasProfilePhoto)
                .hasIdProof(hasIdProof)
                .hasIncomeProof(hasIncomeProof)
                .createdAt(profile.getCreatedAt())
                .build();
    }
}