package com.ktx.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg"
    );

    private final Path rootUploadPath;

    public FileStorageService(@Value("${ktx.upload.dir:uploads}") String uploadDir) {
        this.rootUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootUploadPath.resolve("buildings"));
            Files.createDirectories(this.rootUploadPath.resolve("rooms"));
            log.info("Initialized FileStorageService at root: {}", this.rootUploadPath);
        } catch (IOException e) {
            log.error("Could not initialize upload directories at: {}", this.rootUploadPath, e);
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }

    public Path getRootUploadPath() {
        return rootUploadPath;
    }

    /**
     * Stores a multipart file and returns the web-accessible URL (e.g. /uploads/buildings/abc.webp).
     */
    public String storeFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được rỗng.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ (chỉ nhận JPG, PNG, WEBP, GIF).");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file định dạng hình ảnh.");
        }

        try {
            Path targetDir = rootUploadPath.resolve(subDir).normalize();
            if (!targetDir.startsWith(rootUploadPath)) {
                throw new SecurityException("Đường dẫn lưu trữ không an toàn.");
            }
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path destinationFile = targetDir.resolve(uniqueFilename).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + subDir + "/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new RuntimeException("Lỗi khi lưu trữ file tải lên.", e);
        }
    }

    /**
     * Deletes a file by its relative web URL (e.g. /uploads/buildings/abc.webp).
     */
    public void deleteFile(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith("/uploads/")) {
            return;
        }

        try {
            String pathPart = relativeUrl.substring("/uploads/".length());
            Path targetFile = rootUploadPath.resolve(pathPart).normalize();
            if (targetFile.startsWith(rootUploadPath)) {
                Files.deleteIfExists(targetFile);
                log.info("Deleted physical file: {}", targetFile);
            }
        } catch (IOException e) {
            log.warn("Could not delete file at: {}", relativeUrl, e);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex);
    }
}
