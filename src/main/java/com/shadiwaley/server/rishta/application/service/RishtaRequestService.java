package com.shadiwaley.server.rishta.application.service;

import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.dto.request.CreateRishtaRequest;
import com.shadiwaley.server.rishta.dto.response.RishtaRequestResponse;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.domain.UserSide;
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
public class RishtaRequestService {

    private final RishtaRequestRepository rishtaRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final MediaFileRepository mediaFileRepository;

    private final com.shadiwaley.server.notification.application.service.NotificationService notificationService;
    private final com.shadiwaley.server.chat.application.service.FamilyChatService familyChatService;

    @Transactional
    public void sendRequest(CreateRishtaRequest request) {

        UUID senderUserId = AuthUser.getCurrentUserId();

        UserAccount senderUser = getUser(senderUserId);

        UserProfile senderProfile = getProfileByUserId(senderUserId);

        UserProfile receiverProfile = userProfileRepository.findById(request.getReceiverProfileId())
                .orElseThrow(() -> new EntityNotFoundException("Receiver profile not found"));

        UserAccount receiverUser = receiverProfile.getUserAccount();

        validateRequest(senderUser, senderProfile, receiverUser, receiverProfile);

        boolean exists = rishtaRequestRepository
                .findBySenderUserIdAndReceiverUserIdAndStatusIn(
                        senderUserId,
                        receiverUser.getId(),
                        List.of(
                                RishtaRequestStatus.PENDING,
                                RishtaRequestStatus.ACCEPTED
                        )
                )
                .isPresent();

        if (exists) {
            throw new IllegalArgumentException("You already have an active rishta request with this profile");
        }

        RishtaRequest rishta = new RishtaRequest();

        rishta.setSenderUser(senderUser);
        rishta.setReceiverUser(receiverUser);

        rishta.setSenderProfile(senderProfile);
        rishta.setReceiverProfile(receiverProfile);

        rishta.setSenderNote(request.getSenderNote());

        RishtaRequest saved = rishtaRequestRepository.save(rishta);

        notificationService.create(
                receiverUser.getId(),
                NotificationType.RISHTA_RECEIVED,
                "New rishta request received",
                "A family has shown interest in your profile. Review the request and respond when you are ready.",
                "/rishta/received",
                saved.getId()
        );
    }

    public List<RishtaRequestResponse> getSentRequests() {

        UUID userId = AuthUser.getCurrentUserId();

        return rishtaRequestRepository.findBySenderUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSentResponse)
                .toList();
    }

    public List<RishtaRequestResponse> getReceivedRequests() {

        UUID userId = AuthUser.getCurrentUserId();

        return rishtaRequestRepository.findByReceiverUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toReceivedResponse)
                .toList();
    }

    @Transactional
    public void accept(UUID requestId) {

        UUID currentUserId = AuthUser.getCurrentUserId();

        RishtaRequest request = getRequest(requestId);

        if (!request.getReceiverUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to accept this request");
        }

        if (request.getStatus() != RishtaRequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be accepted");
        }

        request.setStatus(RishtaRequestStatus.ACCEPTED);
        request.setAcceptedAt(Instant.now());
        request.setChatEnabled(true);

        RishtaRequest saved = rishtaRequestRepository.save(request);
        familyChatService.createRoomForAcceptedRishta(saved);

        notificationService.create(
                request.getSenderUser().getId(),
                NotificationType.RISHTA_ACCEPTED,
                "Rishta request accepted",
                "Good news. Your rishta request has been accepted. You can now continue the next step with the family.",
                "/rishta/sent",
                request.getId()
        );    }

    @Transactional
    public void reject(UUID requestId) {

        UUID currentUserId = AuthUser.getCurrentUserId();

        RishtaRequest request = getRequest(requestId);

        if (!request.getReceiverUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to reject this request");
        }

        if (request.getStatus() != RishtaRequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be rejected");
        }

        request.setStatus(RishtaRequestStatus.REJECTED);
        request.setRejectedAt(Instant.now());

        rishtaRequestRepository.save(request);

        notificationService.create(
                request.getSenderUser().getId(),
                NotificationType.RISHTA_REJECTED,
                "Rishta request not accepted",
                "This family is not moving forward right now. Keep your profile active so we can help you find better matches.",
                "/rishta/sent",
                request.getId()
        );
    }

    private void validateRequest(
            UserAccount senderUser,
            UserProfile senderProfile,
            UserAccount receiverUser,
            UserProfile receiverProfile
    ) {

        if (senderUser.getId().equals(receiverUser.getId())) {
            throw new IllegalArgumentException("You cannot send rishta request to yourself");
        }

        if (senderProfile.getProfileStatus() != ProfileStatus.LIVE) {
            throw new IllegalArgumentException("Your profile must be live before sending rishta requests");
        }

        if (receiverProfile.getProfileStatus() != ProfileStatus.LIVE) {
            throw new IllegalArgumentException("Receiver profile is not available");
        }

        if (senderUser.getSide() == receiverUser.getSide()) {
            throw new IllegalArgumentException("Rishta requests are allowed only between opposite profile types");
        }
    }

    private RishtaRequestResponse toSentResponse(RishtaRequest request) {

        UserProfile profile = request.getReceiverProfile();

        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(profile.getUserAccount().getId())
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        return RishtaRequestResponse.builder()
                .requestId(request.getId())
                .profileId(profile.getId())
                .displayId(profile.getDisplayId())
                .candidateFirstName(profile.getCandidateFirstName())
                .candidateAge(profile.getCandidateAge())
                .district(parent.getDistrict())
                .state(parent.getState())
                .professionTitle(profile.getProfessionTitle())
                .education(profile.getEducation())
                .hasApprovedPhoto(hasApprovedProfilePhoto(profile.getId()))
                .status(request.getStatus())
                .senderNote(request.getSenderNote())
                .chatEnabled(request.isChatEnabled())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private RishtaRequestResponse toReceivedResponse(RishtaRequest request) {

        UserProfile profile = request.getSenderProfile();

        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(profile.getUserAccount().getId())
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        return RishtaRequestResponse.builder()
                .requestId(request.getId())
                .profileId(profile.getId())
                .displayId(profile.getDisplayId())
                .candidateFirstName(profile.getCandidateFirstName())
                .candidateAge(profile.getCandidateAge())
                .district(parent.getDistrict())
                .state(parent.getState())
                .professionTitle(profile.getProfessionTitle())
                .education(profile.getEducation())
                .hasApprovedPhoto(hasApprovedProfilePhoto(profile.getId()))
                .status(request.getStatus())
                .senderNote(request.getSenderNote())
                .chatEnabled(request.isChatEnabled())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private boolean hasApprovedProfilePhoto(UUID profileId) {
        return mediaFileRepository
                .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                        profileId,
                        MediaType.PROFILE_PHOTO
                )
                .filter(media -> media.getReviewStatus() == MediaReviewStatus.APPROVED)
                .isPresent();
    }

    private UserAccount getUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private UserProfile getProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
    }

    private RishtaRequest getRequest(UUID requestId) {
        return rishtaRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Rishta request not found"));
    }
}