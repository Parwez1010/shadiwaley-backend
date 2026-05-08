package com.shadiwaley.server.media.application.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String folder);
}