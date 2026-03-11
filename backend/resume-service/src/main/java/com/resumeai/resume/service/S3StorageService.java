package com.resumeai.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Local-filesystem implementation of file storage.
 * Stores files under LOCAL_STORAGE_PATH (default: ~/.resumeai-storage).
 * Drop-in replacement for an S3-based implementation — swap this class when
 * deploying to production.
 */
@Service
@Slf4j
public class S3StorageService {

    private final Path storageRoot;

    public S3StorageService(
            @Value("${local.storage.path:${user.home}/.resumeai-storage}") String storagePath) {
        this.storageRoot = Path.of(storagePath);
        try {
            Files.createDirectories(storageRoot);
            log.info("Local storage root: {}", storageRoot.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot create local storage directory: " + storagePath, e);
        }
    }

    public String uploadResume(MultipartFile file, String userId) {
        String key = "resumes/" + userId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            write(key, file.getBytes());
            log.info("Stored resume locally: {}", key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store resume file", e);
        }
    }

    public String uploadLatex(String latex, String userId, UUID resumeId) {
        String key = "latex/" + userId + "/" + resumeId + ".tex";
        write(key, latex.getBytes());
        return key;
    }

    public String uploadPdf(byte[] pdf, String userId, UUID resumeId) {
        String key = "pdfs/" + userId + "/" + resumeId + ".pdf";
        write(key, pdf);
        return key;
    }

    public byte[] downloadFile(String key) {
        try {
            return Files.readAllBytes(storageRoot.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + key, e);
        }
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void write(String key, byte[] data) {
        try {
            Path target = storageRoot.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + key, e);
        }
    }
}
