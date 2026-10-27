package com.example.bookstore.service;

import com.example.bookstore.exception.BusinessLogicException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * Upload a file and return the file path
     * @param file the file to upload
     * @param category category of the file (e.g., "books", "banners", "avatars")
     * @return the relative path of the uploaded file
     */
    public String uploadFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessLogicException("File cannot be empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessLogicException("File size exceeds maximum limit of 5MB");
        }

        // Validate file extension
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new BusinessLogicException("Invalid file name");
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
        if (!isAllowedExtension(fileExtension)) {
            throw new BusinessLogicException("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        try {
            // Create project-root directory if not exists: ./uploads/banners/
            Path projectRoot = Paths.get("", "").toAbsolutePath().normalize();
            Path uploadPath = projectRoot.resolve(uploadDir).resolve(category).normalize();
            Files.createDirectories(uploadPath);

            // Generate unique file name
            String uniqueFileName = UUID.randomUUID() + "." + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFileName);

            // Save file
            Files.write(filePath, file.getBytes());

            // Return relative path for storage in DB and later public access via /uploads/**
            return "uploads/" + category + "/" + uniqueFileName;
        } catch (IOException e) {
            throw new BusinessLogicException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public String uploadBase64File(String base64Data, String requestedFileName, String category) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new BusinessLogicException("Cover image data cannot be empty");
        }
        if (requestedFileName == null || requestedFileName.isBlank()) {
            throw new BusinessLogicException("File name is required");
        }

        String[] parts = base64Data.split(",", 2);
        if (parts.length != 2) {
            throw new BusinessLogicException("Invalid base64 image data");
        }

        String metadata = parts[0];
        String encodedData = parts[1];

        String fileExtension = extractExtensionFromMetadata(metadata);
        if (fileExtension == null) {
            fileExtension = extractExtensionFromFileName(requestedFileName);
        }
        if (fileExtension == null || !isAllowedExtension(fileExtension)) {
            throw new BusinessLogicException("File type is not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String sanitizedFileName = sanitizeFileName(requestedFileName);
        String uniqueFileName = UUID.randomUUID() + "-" + sanitizedFileName;
        if (!uniqueFileName.toLowerCase().endsWith("." + fileExtension)) {
            uniqueFileName = uniqueFileName + "." + fileExtension;
        }

        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedData);
            if (decodedBytes.length > MAX_FILE_SIZE) {
                throw new BusinessLogicException("File size exceeds maximum limit of 5MB");
            }

            Path uploadPath = Paths.get(uploadDir, category);
            Files.createDirectories(uploadPath);
            Path targetPath = uploadPath.resolve(uniqueFileName);
            Files.write(targetPath, decodedBytes);

            return category + "/" + uniqueFileName;
        } catch (IllegalArgumentException e) {
            throw new BusinessLogicException("Invalid base64 image data", e);
        } catch (IOException e) {
            throw new BusinessLogicException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    private String extractExtensionFromMetadata(String metadata) {
        if (metadata == null || !metadata.contains("data:") || !metadata.contains(";base64")) {
            return null;
        }
        int slashIndex = metadata.indexOf('/');
        int semicolonIndex = metadata.indexOf(';');
        if (slashIndex < 0 || semicolonIndex < 0 || semicolonIndex < slashIndex) {
            return null;
        }
        return metadata.substring(slashIndex + 1, semicolonIndex).toLowerCase();
    }

    private String extractExtensionFromFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9_\\.-]", "_");
    }

    /**
     * Delete a file
     * @param filePath the relative path of the file to delete
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        try {
            Path path = Paths.get(uploadDir, filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new BusinessLogicException("Failed to delete file: " + e.getMessage());
        }
    }

    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
