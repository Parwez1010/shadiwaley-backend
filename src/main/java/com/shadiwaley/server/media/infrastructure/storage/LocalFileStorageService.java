package com.shadiwaley.server.media.infrastructure.storage;

import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.media.application.storage.StoredFile;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.storage.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileStorageService implements FileStorageService {

    @PostConstruct
    public void init() {
        System.out.println("ACTIVE STORAGE PROVIDER: LOCAL");
    }


    @Value("${app.storage.local-upload-dir}")
    private String localUploadDir;

    @Override
    public StoredFile store(MultipartFile file, String folder) {
        try {
            String originalFileName = sanitizeFileName(file.getOriginalFilename());
            String extension = extractExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + extension;

            Path folderPath = Paths.get(localUploadDir, folder).toAbsolutePath().normalize();
            Files.createDirectories(folderPath);

            Path targetPath = folderPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String storageKey = folder + "/" + storedFileName;

            return new StoredFile(
                    originalFileName,
                    storedFileName,
                    storageKey,
                    file.getContentType(),
                    file.getSize()
            );

        } catch (IOException ex) {
            throw new IllegalArgumentException("File upload failed. Please try again.");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "uploaded-file";
        }

        return Paths.get(fileName).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex == -1) {
            return "";
        }

        return fileName.substring(dotIndex);
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(storageKey).normalize();

            if (!java.nio.file.Files.exists(path)) {
                throw new IllegalArgumentException("File not found");
            }

            return java.nio.file.Files.readAllBytes(path);

        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read file");
        }
    }
}