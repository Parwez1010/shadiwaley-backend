package com.shadiwaley.server.media.application.service;

import com.shadiwaley.server.chat.dto.response.AdminChatMediaUploadResponse;
import com.shadiwaley.server.engagement.application.service.MilestoneService;
import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.media.application.storage.StoredFile;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.domain.MediaVisibility;
import com.shadiwaley.server.media.domain.WhatsappConsent;
import com.shadiwaley.server.media.dto.response.*;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.application.service.ProfileCompletionService;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_DOCUMENT_SIZE_BYTES = 8 * 1024 * 1024;

    private final FileStorageService fileStorageService;
    private final MediaFileRepository mediaFileRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ProfileCompletionService profileCompletionService;
    private final MilestoneService milestoneService;

    private final com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository rishtaRequestRepository;

    @Transactional
    public MediaUploadResponse upload(
            MultipartFile file,
            MediaType mediaType,
            WhatsappConsent whatsappConsent
    ) {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile userProfile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        validateFile(file, mediaType);

        WhatsappConsent resolvedConsent = resolveConsent(mediaType, whatsappConsent);
        MediaVisibility visibility = resolveVisibility(mediaType, resolvedConsent);
        boolean isPrimary = mediaType == MediaType.PROFILE_PHOTO;

        if (isPrimary) {
            mediaFileRepository
                    .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                            userProfile.getId(),
                            MediaType.PROFILE_PHOTO
                    )
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        mediaFileRepository.save(existing);
                    });
        }

        String folder = resolveMediaFolder(userAccount.getId(), mediaType);

        StoredFile storedFile = fileStorageService.store(file, folder);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUserAccount(userAccount);
        mediaFile.setUserProfile(userProfile);
        mediaFile.setMediaType(mediaType);
        mediaFile.setOriginalFileName(storedFile.originalFileName());
        mediaFile.setStoredFileName(storedFile.storedFileName());
        mediaFile.setStorageKey(storedFile.storageKey());
        mediaFile.setContentType(storedFile.contentType());
        mediaFile.setFileSizeBytes(storedFile.fileSizeBytes());
        mediaFile.setPrimary(isPrimary);
        mediaFile.setVisibility(visibility);
        mediaFile.setWhatsappConsent(resolvedConsent);
        mediaFile.setReviewStatus(MediaReviewStatus.PENDING_REVIEW);

        MediaFile saved = mediaFileRepository.save(mediaFile);

        recalculateProfileCompletion(userId, userAccount, userProfile);

        return toResponse(saved);
    }

    public List<MediaUploadResponse> getMyFiles() {
        UUID userId = AuthUser.getCurrentUserId();

        return mediaFileRepository.findByUserAccountIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public GroupedMediaResponse getGroupedMedia() {
        UUID userId = AuthUser.getCurrentUserId();

        List<MediaFile> all = mediaFileRepository
                .findByUserAccountIdAndDeletedFalseOrderByCreatedAtDesc(userId);

        MediaUploadResponse profilePhoto = all.stream()
                .filter(m -> m.getMediaType() == MediaType.PROFILE_PHOTO)
                .findFirst()
                .map(this::toResponse)
                .orElse(null);

        List<MediaUploadResponse> gallery = all.stream()
                .filter(m -> m.getMediaType() == MediaType.GALLERY_PHOTO)
                .map(this::toResponse)
                .toList();

        List<MediaUploadResponse> documents = all.stream()
                .filter(m ->
                        m.getMediaType() == MediaType.ID_PROOF
                                || m.getMediaType() == MediaType.INCOME_PROOF
                                || m.getMediaType() == MediaType.OTHER
                )
                .map(this::toResponse)
                .toList();

        return GroupedMediaResponse.builder()
                .profilePhoto(profilePhoto)
                .galleryPhotos(gallery)
                .documents(documents)
                .build();
    }

    @Transactional
    public void deleteMedia(UUID mediaId) {
        UUID userId = AuthUser.getCurrentUserId();

        MediaFile mediaFile = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!mediaFile.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete another user's media");
        }

        mediaFile.setDeleted(true);
        mediaFile.setDeletedAt(Instant.now());
        mediaFile.setPrimary(false);

        mediaFileRepository.save(mediaFile);

        recalculateProfileCompletion(userId, mediaFile.getUserAccount(), mediaFile.getUserProfile());
    }

    @Transactional
    public MediaUploadResponse updateConsent(UUID mediaId, WhatsappConsent whatsappConsent) {
        UUID userId = AuthUser.getCurrentUserId();

        MediaFile mediaFile = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!mediaFile.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot update another user's media");
        }

        if (mediaFile.getMediaType() != MediaType.PROFILE_PHOTO
                && mediaFile.getMediaType() != MediaType.GALLERY_PHOTO) {
            throw new IllegalArgumentException("Consent can only be updated for candidate photos");
        }

        mediaFile.setWhatsappConsent(whatsappConsent);
        mediaFile.setVisibility(resolveVisibility(mediaFile.getMediaType(), whatsappConsent));

        return toResponse(mediaFileRepository.save(mediaFile));
    }

    @Transactional
    public MediaUploadResponse makePrimary(UUID mediaId) {
        UUID userId = AuthUser.getCurrentUserId();

        MediaFile mediaFile = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!mediaFile.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot modify another user's media");
        }

        if (mediaFile.getMediaType() != MediaType.GALLERY_PHOTO
                && mediaFile.getMediaType() != MediaType.PROFILE_PHOTO) {
            throw new IllegalArgumentException("Only candidate photos can be made primary");
        }

        mediaFileRepository.findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                mediaFile.getUserProfile().getId(),
                MediaType.PROFILE_PHOTO
        ).ifPresent(existing -> {
            existing.setPrimary(false);
            mediaFileRepository.save(existing);
        });

        mediaFile.setPrimary(true);
        mediaFile.setMediaType(MediaType.PROFILE_PHOTO);
        mediaFile.setVisibility(resolveVisibility(MediaType.PROFILE_PHOTO, mediaFile.getWhatsappConsent()));
        mediaFile.setReviewStatus(MediaReviewStatus.PENDING_REVIEW);

        MediaFile saved = mediaFileRepository.save(mediaFile);

        recalculateProfileCompletion(userId, mediaFile.getUserAccount(), mediaFile.getUserProfile());

        return toResponse(saved);
    }

    private void recalculateProfileCompletion(
            UUID userId,
            UserAccount userAccount,
            UserProfile userProfile
    ) {
        ParentProfile parentProfile = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        profileCompletionService.recalculateAndApplyForCustomerOnboarding(userAccount, userProfile, parentProfile);        milestoneService.evaluateMilestones(userId);

        userProfileRepository.save(userProfile);
    }

    private void validateFile(MultipartFile file, MediaType mediaType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (mediaType == null) {
            throw new IllegalArgumentException("Media type is required");
        }

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Unable to detect file type");
        }

        if (isPhoto(mediaType)) {
            validateImageFile(file, contentType);
            return;
        }

        if (isDocument(mediaType)) {
            validateDocumentFile(file, contentType);
            return;
        }

        throw new IllegalArgumentException("Unsupported media type");
    }


    private void validateImageFile(MultipartFile file, String contentType) {
        boolean allowed =
                contentType.equals("image/jpeg")
                        || contentType.equals("image/jpg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp");

        if (!allowed) {
            throw new IllegalArgumentException("Only JPG, PNG, and WEBP images are allowed");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image size must be less than 5MB");
        }
    }


    private void validateDocumentFile(MultipartFile file, String contentType) {
        boolean allowed = contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("application/pdf");

        if (!allowed) {
            throw new IllegalArgumentException("Only JPG, PNG, or PDF files are allowed for documents");
        }

        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            throw new IllegalArgumentException("Document size must be less than 8MB");
        }
    }

    private boolean isPhoto(MediaType mediaType) {
        return mediaType == MediaType.PROFILE_PHOTO
                || mediaType == MediaType.GALLERY_PHOTO
                || mediaType == MediaType.CHAT_IMAGE;
    }

    private boolean isDocument(MediaType mediaType) {
        return mediaType == MediaType.ID_PROOF
                || mediaType == MediaType.INCOME_PROOF
                || mediaType == MediaType.OTHER
                || mediaType == MediaType.CHAT_DOCUMENT;
    }

    private WhatsappConsent resolveConsent(MediaType mediaType, WhatsappConsent consent) {
        if (mediaType == MediaType.PROFILE_PHOTO || mediaType == MediaType.GALLERY_PHOTO) {
            return consent == null ? WhatsappConsent.AFTER_ACCEPT : consent;
        }

        return null;
    }

    private MediaVisibility resolveVisibility(MediaType mediaType, WhatsappConsent consent) {
        if (mediaType == MediaType.ID_PROOF || mediaType == MediaType.INCOME_PROOF) {
            return MediaVisibility.INTERNAL_ONLY;
        }

        if (mediaType == MediaType.PROFILE_PHOTO) {
            return MediaVisibility.CHAT_SHAREABLE;
        }

        if (mediaType == MediaType.GALLERY_PHOTO) {
            if (consent == WhatsappConsent.ALWAYS) {
                return MediaVisibility.AUTOPILOT_SHAREABLE;
            }

            return MediaVisibility.CHAT_SHAREABLE;
        }

        return MediaVisibility.PRIVATE;
    }

    private MediaUploadResponse toResponse(MediaFile mediaFile) {

        UUID userId = mediaFile.getUserAccount().getId();

        return MediaUploadResponse.builder()
                .mediaId(mediaFile.getId())
                .userId(userId)
                .profileId(mediaFile.getUserProfile().getId())

                .mediaType(mediaFile.getMediaType())

                .fileName(mediaFile.getOriginalFileName())
                .contentType(mediaFile.getContentType())

                .size(mediaFile.getFileSizeBytes())
                .fileSizeBytes(mediaFile.getFileSizeBytes())

                .primary(mediaFile.isPrimary())

                .visibility(mediaFile.getVisibility())
                .whatsappConsent(mediaFile.getWhatsappConsent())

                .verificationStatus(mediaFile.getReviewStatus())
                .reviewStatus(mediaFile.getReviewStatus())

                .rejectedReason(null)
                .mimeType(mediaFile.getContentType())

                .originalFileName(mediaFile.getOriginalFileName())

                .fileSize(mediaFile.getFileSizeBytes())

                .approvalStatus(mediaFile.getReviewStatus())

                .previewUrl(
                        "/api/v1/media/" +
                                mediaFile.getId() +
                                "/view"
                )

                .adminPreviewUrl(
                        "/api/v1/admin/crm/families/"
                                + userId
                                + "/media/"
                                + mediaFile.getId()
                                + "/view"
                )

                .uploadedAt(mediaFile.getCreatedAt())
                .createdAt(mediaFile.getCreatedAt())

                .build();
    }

    public MediaViewResponse viewMedia(UUID mediaId) {
        UUID currentUserId = AuthUser.getCurrentUserId();

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        boolean owner = media.getUserAccount().getId().equals(currentUserId);
        boolean connectedFamily = isConnectedFamily(currentUserId, media.getUserAccount().getId());

        if (!owner && !connectedFamily) {
            throw new IllegalArgumentException("You are not allowed to view this media");
        }

        byte[] content = fileStorageService.load(media.getStorageKey());

        return MediaViewResponse.builder()
                .fileName(media.getOriginalFileName())
                .contentType(media.getContentType())
                .content(content)
                .build();
    }

    private boolean isConnectedFamily(UUID currentUserId, UUID mediaOwnerUserId) {
        return rishtaRequestRepository
                .existsBySenderUserIdAndReceiverUserIdAndStatus(
                        currentUserId,
                        mediaOwnerUserId,
                        RishtaRequestStatus.ACCEPTED
                )
                || rishtaRequestRepository
                .existsBySenderUserIdAndReceiverUserIdAndStatus(
                        mediaOwnerUserId,
                        currentUserId,
                        RishtaRequestStatus.ACCEPTED
                );
    }

    @Transactional(readOnly = true)
    public MediaViewResponse viewAdminMedia(UUID mediaId) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        byte[] content = fileStorageService.load(media.getStorageKey());

        return MediaViewResponse.builder()
                .fileName(media.getOriginalFileName())
                .contentType(media.getContentType())
                .content(content)
                .build();
    }

    @Transactional
    public MediaUploadResponse uploadMediaForUser(
            UUID userId,
            MediaType mediaType,
            boolean primary,
            WhatsappConsent whatsappConsent,
            MultipartFile file
    ) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile userProfile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        validateFile(file, mediaType);

        if (primary) {
            mediaFileRepository
                    .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                            userProfile.getId(),
                            MediaType.PROFILE_PHOTO
                    )
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        mediaFileRepository.save(existing);
                    });
        }

        WhatsappConsent resolvedConsent = resolveConsent(mediaType, whatsappConsent);
        MediaVisibility visibility = resolveVisibility(mediaType, resolvedConsent);

        String folder = resolveMediaFolder(userAccount.getId(), mediaType);

        StoredFile storedFile = fileStorageService.store(file, folder);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUserAccount(userAccount);
        mediaFile.setUserProfile(userProfile);
        mediaFile.setMediaType(primary ? MediaType.PROFILE_PHOTO : mediaType);
        mediaFile.setOriginalFileName(storedFile.originalFileName());
        mediaFile.setStoredFileName(storedFile.storedFileName());
        mediaFile.setStorageKey(storedFile.storageKey());
        mediaFile.setContentType(storedFile.contentType());
        mediaFile.setFileSizeBytes(storedFile.fileSizeBytes());
        mediaFile.setPrimary(primary);
        mediaFile.setWhatsappConsent(resolvedConsent);
        mediaFile.setVisibility(visibility);
        mediaFile.setReviewStatus(MediaReviewStatus.PENDING_REVIEW);
        mediaFile.setDeleted(false);

        MediaFile saved = mediaFileRepository.save(mediaFile);

        recalculateProfileCompletion(
                userId,
                userAccount,
                userProfile
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MediaUploadResponse> getFamilyMedia(UUID userId) {

        return mediaFileRepository
                .findByUserAccountIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MediaViewResponse viewFamilyMedia(UUID userId, UUID mediaId) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!media.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("Media does not belong to this family");
        }

        byte[] content = fileStorageService.load(media.getStorageKey());

        return MediaViewResponse.builder()
                .fileName(media.getOriginalFileName())
                .contentType(media.getContentType())
                .content(content)
                .build();
    }

    @Transactional
    public void deleteFamilyMedia(UUID userId, UUID mediaId) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!media.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("Media does not belong to this family");
        }

        media.setDeleted(true);
        media.setDeletedAt(Instant.now());
        media.setPrimary(false);

        mediaFileRepository.save(media);

        recalculateProfileCompletion(
                userId,
                media.getUserAccount(),
                media.getUserProfile()
        );
    }

    @Transactional
    public MediaUploadResponse setPrimaryFamilyMedia(UUID userId, UUID mediaId) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!media.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("Media does not belong to this family");
        }

        if (media.getMediaType() != MediaType.PROFILE_PHOTO
                && media.getMediaType() != MediaType.GALLERY_PHOTO) {
            throw new IllegalArgumentException("Only candidate photos can be marked as primary");
        }

        mediaFileRepository
                .findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
                        media.getUserProfile().getId(),
                        MediaType.PROFILE_PHOTO
                )
                .ifPresent(existing -> {
                    existing.setPrimary(false);
                    mediaFileRepository.save(existing);
                });

        media.setPrimary(true);
        media.setMediaType(MediaType.PROFILE_PHOTO);
        media.setVisibility(
                resolveVisibility(
                        MediaType.PROFILE_PHOTO,
                        media.getWhatsappConsent()
                )
        );

        media.setReviewStatus(MediaReviewStatus.PENDING_REVIEW);

        MediaFile saved = mediaFileRepository.save(media);

        recalculateProfileCompletion(
                userId,
                media.getUserAccount(),
                media.getUserProfile()
        );

        return toResponse(saved);
    }

    @Transactional
    public MediaUploadResponse updateFamilyMediaWhatsappConsent(
            UUID userId,
            UUID mediaId,
            WhatsappConsent whatsappConsent
    ) {

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

        if (!media.getUserAccount().getId().equals(userId)) {
            throw new IllegalArgumentException("Media does not belong to this family");
        }

        if (media.getMediaType() != MediaType.PROFILE_PHOTO
                && media.getMediaType() != MediaType.GALLERY_PHOTO) {
            throw new IllegalArgumentException("Consent can only be updated for candidate photos");
        }

        media.setWhatsappConsent(whatsappConsent);

        media.setVisibility(
                resolveVisibility(
                        media.getMediaType(),
                        whatsappConsent
                )
        );

        MediaFile saved = mediaFileRepository.save(media);

        return toResponse(saved);
    }

    private String resolveMediaFolder(UUID userId, MediaType mediaType) {
        return switch (mediaType) {
            case PROFILE_PHOTO -> "families/" + userId + "/profile-photos";
            case GALLERY_PHOTO -> "families/" + userId + "/gallery";
            case ID_PROOF -> "families/" + userId + "/id-proofs";
            case INCOME_PROOF -> "families/" + userId + "/income-proofs";
            case CHAT_IMAGE -> "families/" + userId + "/chat/images";
            case CHAT_DOCUMENT -> "families/" + userId + "/chat/documents";
            case OTHER -> "families/" + userId + "/other";
        };
    }

    public MediaOptionsResponse getOptions() {

        return MediaOptionsResponse.builder()
                .mediaTypes(
                        Arrays.stream(MediaType.values())
                                .map(Enum::name)
                                .toList()
                )
                .whatsappConsents(
                        Arrays.stream(WhatsappConsent.values())
                                .map(Enum::name)
                                .toList()
                )
                .mediaVisibilities(
                        Arrays.stream(MediaVisibility.values())
                                .map(Enum::name)
                                .toList()
                )
                .reviewStatuses(
                        Arrays.stream(MediaReviewStatus.values())
                                .map(Enum::name)
                                .toList()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public MediaSummaryResponse getSummary() {
        UUID userId = AuthUser.getCurrentUserId();

        List<MediaFile> files =
                mediaFileRepository.findByUserAccountIdAndDeletedFalseOrderByCreatedAtDesc(userId);

        long profilePhotos = files.stream()
                .filter(file -> file.getMediaType() == MediaType.PROFILE_PHOTO)
                .count();

        long galleryPhotos = files.stream()
                .filter(file -> file.getMediaType() == MediaType.GALLERY_PHOTO)
                .count();

        long documents = files.stream()
                .filter(file ->
                        file.getMediaType() == MediaType.ID_PROOF
                                || file.getMediaType() == MediaType.INCOME_PROOF
                                || file.getMediaType() == MediaType.OTHER
                )
                .count();

        long chatImages = files.stream()
                .filter(file -> file.getMediaType() == MediaType.CHAT_IMAGE)
                .count();

        long chatDocuments = files.stream()
                .filter(file -> file.getMediaType() == MediaType.CHAT_DOCUMENT)
                .count();

        long pendingReview = files.stream()
                .filter(file -> file.getReviewStatus() == MediaReviewStatus.PENDING_REVIEW)
                .count();

        long approved = files.stream()
                .filter(file -> file.getReviewStatus() == MediaReviewStatus.APPROVED)
                .count();

        long rejected = files.stream()
                .filter(file -> file.getReviewStatus() == MediaReviewStatus.REJECTED)
                .count();

        return MediaSummaryResponse.builder()
                .totalFiles((long) files.size())
                .profilePhotos(profilePhotos)
                .galleryPhotos(galleryPhotos)
                .documents(documents)
                .chatImages(chatImages)
                .chatDocuments(chatDocuments)
                .pendingReview(pendingReview)
                .approved(approved)
                .rejected(rejected)
                .build();
    }


    @Transactional
    public AdminChatMediaUploadResponse uploadCrmChatMedia(
            UUID roomId,
            UUID mediaOwnerUserId,
            MultipartFile file
    ) {
        UserAccount userAccount = userAccountRepository.findById(mediaOwnerUserId)
                .orElseThrow(() -> new EntityNotFoundException("Media owner user not found"));

        UserProfile userProfile = userProfileRepository.findByUserAccountId(mediaOwnerUserId)
                .orElseThrow(() -> new EntityNotFoundException("Media owner profile not found"));

        MediaType mediaType = resolveChatMediaType(file);

        validateFile(file, mediaType);

        String folder =
                "families/" +
                        mediaOwnerUserId +
                        "/chat/rooms/" +
                        roomId +
                        "/" +
                        (mediaType == MediaType.CHAT_IMAGE ? "images" : "documents");

        StoredFile storedFile = fileStorageService.store(file, folder);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUserAccount(userAccount);
        mediaFile.setUserProfile(userProfile);
        mediaFile.setMediaType(mediaType);
        mediaFile.setOriginalFileName(storedFile.originalFileName());
        mediaFile.setStoredFileName(storedFile.storedFileName());
        mediaFile.setStorageKey(storedFile.storageKey());
        mediaFile.setContentType(storedFile.contentType());
        mediaFile.setFileSizeBytes(storedFile.fileSizeBytes());
        mediaFile.setPrimary(false);
        mediaFile.setVisibility(MediaVisibility.PRIVATE);
        mediaFile.setWhatsappConsent(null);
        mediaFile.setReviewStatus(MediaReviewStatus.APPROVED);
        mediaFile.setDeleted(false);

        MediaFile saved = mediaFileRepository.save(mediaFile);

        return AdminChatMediaUploadResponse.builder()
                .mediaFileId(saved.getId())
                .fileName(saved.getOriginalFileName())
                .fileSizeBytes(saved.getFileSizeBytes())
                .contentType(saved.getContentType())
                .mediaType(saved.getMediaType())
                .mediaPreviewUrl(
                        "/api/v1/admin/chat-monitor/rooms/"
                                + roomId
                                + "/media/"
                                + saved.getId()
                                + "/view"
                )
                .build();
    }

    private MediaType resolveChatMediaType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Unable to detect file type");
        }

        return switch (contentType) {
            case "image/jpeg",
                 "image/jpg",
                 "image/png",
                 "image/webp" -> MediaType.CHAT_IMAGE;

            case "application/pdf" -> MediaType.CHAT_DOCUMENT;

            default -> throw new IllegalArgumentException(
                    "Only JPG, PNG, WEBP images and PDF documents are allowed"
            );
        };
    }






}