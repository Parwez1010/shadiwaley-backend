package com.shadiwaley.server.engagement.application.service;

import com.shadiwaley.server.engagement.domain.MilestoneCode;
import com.shadiwaley.server.engagement.dto.response.MilestoneResponse;
import com.shadiwaley.server.engagement.infrastructure.entity.ProfileMilestone;
import com.shadiwaley.server.engagement.infrastructure.repository.ProfileMilestoneRepository;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final ProfileMilestoneRepository milestoneRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaFileRepository mediaFileRepository;

    @Transactional
    public void evaluateMilestones(UUID userId) {

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        awardIfNeeded(
                user,
                MilestoneCode.PROFILE_CREATED,
                true,
                "Profile Created",
                "Your profile setup has started successfully."
        );

        awardIfNeeded(
                user,
                MilestoneCode.PROFILE_50_PERCENT,
                profile.getCompletionPct() >= 50,
                "Profile 50% Complete",
                "Your profile is halfway ready."
        );

        awardIfNeeded(
                user,
                MilestoneCode.PROFILE_80_PERCENT,
                profile.getCompletionPct() >= 80,
                "Profile 80% Complete",
                "Your profile is almost ready for review."
        );

        awardIfNeeded(
                user,
                MilestoneCode.PROFILE_READY_FOR_REVIEW,
                profile.getProfileStatus() == ProfileStatus.READY_FOR_REVIEW,
                "Ready For Review",
                "Your profile is ready for verification review."
        );

        awardIfNeeded(
                user,
                MilestoneCode.PROFILE_LIVE,
                profile.getProfileStatus() == ProfileStatus.LIVE,
                "Profile Live",
                "Your profile is now live."
        );

        awardIfNeeded(
                user,
                MilestoneCode.PROFILE_PHOTO_UPLOADED,
                hasMedia(userId, MediaType.PROFILE_PHOTO),
                "Profile Photo Uploaded",
                "Your candidate photo has been uploaded."
        );

        awardIfNeeded(
                user,
                MilestoneCode.ID_PROOF_UPLOADED,
                hasMedia(userId, MediaType.ID_PROOF),
                "ID Proof Uploaded",
                "Your ID verification document has been uploaded."
        );

        awardIfNeeded(
                user,
                MilestoneCode.INCOME_PROOF_UPLOADED,
                hasMedia(userId, MediaType.INCOME_PROOF),
                "Income Proof Uploaded",
                "Your income proof has been uploaded."
        );
    }

    public List<MilestoneResponse> getMyMilestones() {

        UUID userId = AuthUser.getCurrentUserId();

        return milestoneRepository.findByUserAccountIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void awardIfNeeded(
            UserAccount user,
            MilestoneCode code,
            boolean condition,
            String title,
            String description
    ) {

        if (!condition) {
            return;
        }

        boolean exists = milestoneRepository
                .findByUserAccountIdAndMilestoneCode(user.getId(), code)
                .isPresent();

        if (exists) {
            return;
        }

        ProfileMilestone milestone = new ProfileMilestone();

        milestone.setUserAccount(user);
        milestone.setMilestoneCode(code);

        milestone.setTitle(title);
        milestone.setDescription(description);

        milestone.setAchieved(true);
        milestone.setAchievedAt(Instant.now());

        milestoneRepository.save(milestone);
    }

    private boolean hasMedia(UUID userId, MediaType mediaType) {
        return !mediaFileRepository
                .findByUserAccountIdAndMediaTypeAndDeletedFalse(userId, mediaType)
                .isEmpty();
    }

    private MilestoneResponse toResponse(ProfileMilestone milestone) {

        return MilestoneResponse.builder()
                .code(milestone.getMilestoneCode().name())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .achieved(milestone.isAchieved())
                .achievedAt(milestone.getAchievedAt())
                .build();
    }
}