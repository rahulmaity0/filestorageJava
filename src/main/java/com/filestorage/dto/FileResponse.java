package com.filestorage.dto;

import com.filestorage.model.FileMetadata;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileResponse {
    private Long id;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private boolean hasThumbnail;

    public static FileResponse fromFileMetadata(FileMetadata file) {
        FileResponse response = new FileResponse();
        response.setId(file.getId());
        response.setFilename(file.getFilename());
        response.setOriginalFilename(file.getOriginalFilename());
        response.setFileSize(file.getFileSize());
        response.setContentType(file.getContentType());
        response.setUploadedAt(file.getUploadedAt());
        response.setHasThumbnail(file.getThumbnailPath() != null);
        return response;
    }
}
