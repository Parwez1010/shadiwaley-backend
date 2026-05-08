package com.shadiwaley.server.media.infrastructure.repository;

import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

    List<MediaFile> findByUserAccountIdAndDeletedFalseOrderByCreatedAtDesc(UUID userAccountId);

    Optional<MediaFile> findByIdAndDeletedFalse(UUID id);

    Optional<MediaFile> findByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalse(
            UUID userProfileId,
            MediaType mediaType
    );

    List<MediaFile> findByUserAccountIdAndMediaTypeAndDeletedFalse(
            UUID userAccountId,
            MediaType mediaType
    );
}