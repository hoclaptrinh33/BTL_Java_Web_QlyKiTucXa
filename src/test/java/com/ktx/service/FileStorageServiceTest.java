package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    private Path tempUploadDir;
    private FileStorageService storageService;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadDir = Files.createTempDirectory("ktx_test_uploads");
        storageService = new FileStorageService(tempUploadDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempUploadDir)) {
            try (var stream = Files.walk(tempUploadDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {}
                        });
            }
        }
    }

    @Test
    void storeFile_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-building.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        String url = storageService.storeFile(file, "buildings");
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/buildings/"));
        assertTrue(url.endsWith(".jpg"));

        // File should exist on disk
        String relative = url.substring("/uploads/".length());
        Path physical = tempUploadDir.resolve(relative);
        assertTrue(Files.exists(physical));

        // Test deletion
        storageService.deleteFile(url);
        assertFalse(Files.exists(physical));
    }

    @Test
    void storeFile_emptyFile_throwsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.storeFile(emptyFile, "buildings"));
    }

    @Test
    void storeFile_unsupportedExtension_throwsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "danger.exe",
                "image/jpeg",
                "abc".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.storeFile(badFile, "buildings"));
    }

    @Test
    void storeFile_nonImageContentType_throwsException() {
        MockMultipartFile badMime = new MockMultipartFile(
                "file",
                "document.png",
                "application/pdf",
                "abc".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.storeFile(badMime, "buildings"));
    }
}
