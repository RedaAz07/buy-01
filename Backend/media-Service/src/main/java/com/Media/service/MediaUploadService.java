package com.Media.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Media.exceptions.ApiException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadService {

    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public record UploadResult(String url, String publicId) {}

    public UploadResult uploadFile(MultipartFile file) throws IOException {

        String realMimeType = tika.detect(
                file.getInputStream(),
                file.getOriginalFilename()
        );

        // Check mime type
        if (!ALLOWED_MIME_TYPES.contains(realMimeType)) {
            throw ApiException.badRequest("Invalid media uploaded: " + realMimeType);
        }

        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    tempFile,
                    ObjectUtils.asMap("resource_type", "auto")
            );

            return new UploadResult(
                    uploadResult.get("secure_url").toString(),
                    uploadResult.get("public_id").toString()
            );

        } finally {
            if (!tempFile.delete()) {
                log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    public void deleteOrphanedFiles(List<String> publicIds) {
        for (String publicId : publicIds) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Cleaned up orphaned file with public ID: {}", publicId);
            } catch (Exception e) {
                log.error("Failed to clean up Cloudinary file with public ID: {}", publicId, e);
            }
        }
    }
    public void deleteFile(String publicId) {
    try {
        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );

        log.info(
                "Deleted Cloudinary file with public ID: {}",
                publicId
        );

    } catch (Exception e) {
        log.error(
                "Failed to delete Cloudinary file with public ID: {}",
                publicId,
                e
        );

        throw new RuntimeException(
                "Failed to delete Cloudinary file",
                e
        );
    }
}
}