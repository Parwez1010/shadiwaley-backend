package com.shadiwaley.server.engagement.application.service;

import com.shadiwaley.server.engagement.dto.response.TodayActionResponse;
import com.shadiwaley.server.engagement.infrastructure.entity.EngagementLoopState;
import com.shadiwaley.server.engagement.infrastructure.repository.EngagementLoopStateRepository;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.application.service.ProfileCompletionService;
import com.shadiwaley.server.profile.dto.response.MissingFieldResponse;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private final EngagementLoopStateRepository engagementRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final MilestoneService milestoneService;
    private final ProfileCompletionService profileCompletionService;
    private final ParentProfileRepository parentProfileRepository;

    @Transactional
    public void recordSessionStart() {

        UUID userId = AuthUser.getCurrentUserId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        EngagementLoopState state = engagementRepository.findByUserAccountId(userId)
                .orElseGet(() -> {
                    EngagementLoopState created = new EngagementLoopState();
                    created.setUserAccount(user);
                    created.setCurrentStreak(0);
                    created.setMomentumScore(0);
                    created.setEngagementScore(0);
                    created.setProfileViews(0);
                    created.setProfileShares(0);
                    created.setOnboardingCompleted(false);
                    created.setProfileLive(false);
                    return created;
                });

        updateStreak(state);

        state.setLastLoginAt(Instant.now());
        state.setLastActivityAt(Instant.now());

        state.setEngagementScore(defaultValue(state.getEngagementScore()) + 5);
        state.setMomentumScore(defaultValue(state.getMomentumScore()) + 2);

        engagementRepository.save(state);

        milestoneService.evaluateMilestones(userId);
    }

    private int defaultValue(Integer value) {
        return value == null ? 0 : value;
    }

    public TodayActionResponse getTodayAction() {

        UUID userId = AuthUser.getCurrentUserId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        var parent = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        var completion =
                profileCompletionService.buildCurrentResponse(user, profile, parent);

        List<MissingFieldResponse> missingFields =
                completion.getMissingFields();

        if (missingFields == null || missingFields.isEmpty()) {

            return TodayActionResponse.builder()
                    .title("Profile Looks Great")
                    .priority("LOW")
                    .message("Your profile is progressing well. Stay active to receive better engagement.")
                    .actionCode("KEEP_ACTIVE")
                    .build();
        }

        MissingFieldResponse highest = missingFields.get(0);

        return TodayActionResponse.builder()
                .title(highest.getLabel())
                .priority(highest.getPriority())
                .message(highest.getConsequenceMessage())
                .actionCode(highest.getField())
                .build();
    }

    private void updateStreak(EngagementLoopState state) {

        if (state.getLastLoginAt() == null) {
            state.setCurrentStreak(1);
            return;
        }

        LocalDate previousDate =
                state.getLastLoginAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (previousDate.equals(today.minusDays(1))) {
            state.setCurrentStreak(state.getCurrentStreak() + 1);
            return;
        }

        if (!previousDate.equals(today)) {
            state.setCurrentStreak(1);
        }
    }
}