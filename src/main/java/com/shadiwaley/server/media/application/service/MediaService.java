package com.shadiwaley.server.media.application.service;

import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.media.application.storage.StoredFile;
import com.shadiwaley.server.media.domain.MediaReviewStatus;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.domain.MediaVisibility;
import com.shadiwaley.server.media.domain.WhatsappConsent;
import com.shadiwaley.server.media.dto.response.GroupedMediaResponse;
import com.shadiwaley.server.media.dto.response.MediaUploadResponse;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.application.service.ProfileCompletionService;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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

        String folder = userAccount.getId() + "/" + mediaType.name().toLowerCase();
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

        profileCompletionService.recalculateAndApply(userAccount, userProfile, parentProfile);
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
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new IllegalArgumentException("Only JPG and PNG images are allowed for candidate photos");
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
                || mediaType == MediaType.GALLERY_PHOTO;
    }

    private boolean isDocument(MediaType mediaType) {
        return mediaType == MediaType.ID_PROOF
                || mediaType == MediaType.INCOME_PROOF
                || mediaType == MediaType.OTHER;
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
        return MediaUploadResponse.builder()
                .mediaId(mediaFile.getId())
                .mediaType(mediaFile.getMediaType())
                .originalFileName(mediaFile.getOriginalFileName())
                .contentType(mediaFile.getContentType())
                .fileSizeBytes(mediaFile.getFileSizeBytes())
                .primary(mediaFile.isPrimary())
                .visibility(mediaFile.getVisibility())
                .whatsappConsent(mediaFile.getWhatsappConsent())
                .reviewStatus(mediaFile.getReviewStatus())
                .uploadedAt(mediaFile.getCreatedAt())
                .build();
    }
}