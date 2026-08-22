package com.filestorage.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Value("${app.storage.thumbnail-dir}")
    private String thumbnailDir;

    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(thumbnailDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }

    public String generateUniqueFilename(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + ext;
    }

    public String saveFile(MultipartFile file, String uniqueFilename) throws IOException {
        init(); // ensure dirs exist
        Path targetLocation = Paths.get(uploadDir).resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return targetLocation.toString();
    }

    public String createThumbnail(String imagePath, String filename) {
        try {
            init();
            String thumbnailFilename = "thumb_" + filename;
            Path thumbnailPath = Paths.get(thumbnailDir).resolve(thumbnailFilename);
            
            Thumbnails.of(new File(imagePath))
                    .size(200, 200)
                    .toFile(thumbnailPath.toFile());
                    
            return thumbnailPath.toString();
        } catch (IOException e) {
            System.err.println("Error creating thumbnail: " + e.getMessage());
            return null;
        }
    }

    public void deleteFile(String filePath) {
        if (filePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }
    }

    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
