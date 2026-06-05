package com.shadiwaley.server.media.infrastructure.storage;

import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.media.application.storage.StoredFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
public class CloudflareR2StorageService implements FileStorageService {

    @PostConstruct
    public void init() {
        System.out.println("ACTIVE STORAGE PROVIDER: CLOUDFLARE R2");
    }

    private final S3Client r2S3Client;
    private final R2StorageProperties properties;

    @Override
    public StoredFile store(MultipartFile file, String folder) {
        try {
            String originalFileName = sanitizeFileName(file.getOriginalFilename());
            String extension = extractExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + extension;
            String storageKey = normalizeFolder(folder) + "/" + storedFileName;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(storageKey)
                    .contentType(resolveContentType(file))
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                            "original-file-name", originalFileName
                    ))
                    .build();

            r2S3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return new StoredFile(
                    originalFileName,
                    storedFileName,
                    storageKey,
                    resolveContentType(file),
                    file.getSize()
            );

        } catch (Exception ex) {
            throw new IllegalArgumentException("File upload failed. Please try again.");
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(storageKey)
                    .build();

            ResponseBytes<GetObjectResponse> response =
                    r2S3Client.getObjectAsBytes(request);

            return response.asByteArray();

        } catch (NoSuchKeyException ex) {
            throw new IllegalArgumentException("File not found");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read file");
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

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "misc";
        }

        return folder
                .replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String resolveContentType(MultipartFile file) {
        return file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";
    }
}