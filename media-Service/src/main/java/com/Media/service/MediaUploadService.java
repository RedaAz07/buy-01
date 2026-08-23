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

@Service
public class MediaUploadService {

    private final Cloudinary cloudinary;

    private final Tika tika = new Tika();


    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    public MediaUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadFile(MultipartFile file) throws IOException {

        

        String realMimeType = tika.detect(
                file.getInputStream(),
                file.getOriginalFilename());

        // check mime type
        if (!ALLOWED_MIME_TYPES.contains(realMimeType)) {
            throw ApiException.badRequest(
                    "Invalid media uploaded: " + realMimeType);
        }

        File tempFile = File.createTempFile(
                "upload-",
                file.getOriginalFilename());

        file.transferTo(tempFile);

        try {

            Map uploadResult = cloudinary.uploader().upload(
                    tempFile,
                    ObjectUtils.asMap(
                            "resource_type", "auto"));

            return uploadResult.get("secure_url").toString();

        } finally {
            tempFile.delete();
        }
    }

    // Add this to MediaUploadService.java
    public void deleteOrphanedFiles(List<String> urls) {
        for (String url : urls) {
            try {
                // Extract the public ID from the end of the URL
                // Example URL: https://res.cloudinary.com/demo/image/upload/v1234/my_image.jpg
                // Public ID: my_image
                String[] parts = url.split("/");
                String fileWithExt = parts[parts.length - 1];
                String publicId = fileWithExt.substring(0, fileWithExt.lastIndexOf("."));

                // Tell Cloudinary to destroy it
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("Cleaned up orphaned file: " + publicId);
            } catch (Exception e) {
                System.err.println("Failed to clean up Cloudinary file: " + url);
            }
        }
    }
}