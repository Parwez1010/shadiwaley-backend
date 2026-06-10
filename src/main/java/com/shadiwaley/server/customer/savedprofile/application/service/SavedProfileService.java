package com.shadiwaley.server.customer.savedprofile.application.service;

import com.shadiwaley.server.customer.savedprofile.entity.SavedProfile;
import com.shadiwaley.server.customer.savedprofile.infrastructure.repository.SavedProfileRepository;
import com.shadiwaley.server.profile.application.service.ProfileBrowseService;
import com.shadiwaley.server.profile.dto.response.ProfileCardResponse;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedProfileService {

    private final SavedProfileRepository savedProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;

    private final ProfileBrowseService profileBrowseService;

    @Transactional
    public void saveProfile(UUID profileId) {
        UUID userId = AuthUser.getCurrentActorId();

        if (savedProfileRepository.existsByUserAccountIdAndSavedProfileId(userId, profileId)) {
            return;
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        if (profile.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot save your own profile");
        }

        SavedProfile savedProfile = new SavedProfile();
        savedProfile.setUserAccount(user);
        savedProfile.setSavedProfile(profile);

        savedProfileRepository.save(savedProfile);
    }

    @Transactional
    public void removeSavedProfile(UUID profileId) {
        UUID userId = AuthUser.getCurrentActorId();

        savedProfileRepository
                .findByUserAccountIdAndSavedProfileId(userId, profileId)
                .ifPresent(savedProfileRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ProfileCardResponse> getSavedProfiles() {
        UUID userId = AuthUser.getCurrentActorId();

        return savedProfileRepository
                .findByUserAccountIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(saved -> profileBrowseService.getProfileCard(saved.getSavedProfile().getId()))
                .toList();
    }
}