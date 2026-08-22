package com.filestorage.controller;

import com.filestorage.dto.FileResponse;
import com.filestorage.model.FileMetadata;
import com.filestorage.model.User;
import com.filestorage.repository.FileMetadataRepository;
import com.filestorage.repository.UserRepository;
import com.filestorage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Value("${app.storage.max-file-size}")
    private long maxFileSize;

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("File too large. Max size: " + maxFileSize + " bytes");
            }

            User currentUser = getCurrentUser(authentication);
            
            // Save file
            String uniqueFilename = storageService.generateUniqueFilename(file.getOriginalFilename());
            String filePath = storageService.saveFile(file, uniqueFilename);
            
            // Thumbnail
            String thumbnailPath = null;
            if (storageService.isImage(file.getContentType())) {
                thumbnailPath = storageService.createThumbnail(filePath, uniqueFilename);
            }

            // Save metadata
            FileMetadata metadata = new FileMetadata();
            metadata.setFilename(uniqueFilename);
            metadata.setOriginalFilename(file.getOriginalFilename());
            metadata.setFilePath(filePath);
            metadata.setThumbnailPath(thumbnailPath);
            metadata.setFileSize(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setUser(currentUser);

            fileRepository.save(metadata);

            return ResponseEntity.ok(FileResponse.fromFileMetadata(metadata));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload the file: " + e.getMessage());
        }
    }

    @GetMapping("/")
    public ResponseEntity<List<FileResponse>> listFiles(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        List<FileMetadata> files = fileRepository.findByUser(currentUser);
        
        List<FileResponse> responses = files.stream()
                .map(FileResponse::fromFileMetadata)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<?> getFileInfo(@PathVariable Long fileId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        FileMetadata file = fileRepository.findByIdAndUser(fileId, currentUser)
                .orElse(null);
                
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
        
        return ResponseEntity.ok(FileResponse.fromFileMetadata(file));
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable Long fileId, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            FileMetadata fileMetadata = fileRepository.findByIdAndUser(fileId, currentUser)
                    .orElse(null);
                    
            if (fileMetadata == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            Path path = Paths.get(fileMetadata.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(fileMetadata.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadata.getOriginalFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found on disk");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error downloading file: " + e.getMessage());
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        FileMetadata fileMetadata = fileRepository.findByIdAndUser(fileId, currentUser)
                .orElse(null);
                
        if (fileMetadata == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }

        // Delete from disk
        storageService.deleteFile(fileMetadata.getFilePath());
        if (fileMetadata.getThumbnailPath() != null) {
            storageService.deleteFile(fileMetadata.getThumbnailPath());
        }

        // Delete from DB
        fileRepository.delete(fileMetadata);

        return ResponseEntity.ok("File deleted successfully");
    }
}
