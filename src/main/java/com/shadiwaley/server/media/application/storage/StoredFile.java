package com.shadiwaley.server.media.application.storage;

public record StoredFile(
        String originalFileName,
        String storedFileName,
        String storageKey,
        String contentType,
        long fileSizeBytes
) {
}