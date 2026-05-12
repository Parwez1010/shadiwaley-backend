package com.shadiwaley.server.employee.application.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class EmployeeFileStorageService {

    private static final String BASE_DIR = "uploads/employees";

    public String store(MultipartFile file, String folder) {
        try {
            String originalName = file.getOriginalFilename() == null
                    ? "file"
                    : file.getOriginalFilename();

            String extension = "";
            int dotIndex = originalName.lastIndexOf(".");
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }

            String fileName = UUID.randomUUID() + extension;

            Path directory = Paths.get(BASE_DIR, folder).normalize();
            Files.createDirectories(directory);

            Path target = directory.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return target.toString();

        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to store employee file");
        }
    }
}