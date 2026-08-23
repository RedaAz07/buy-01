package com.Media.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Media.exceptions.ApiException;
import com.Media.model.Media;
import com.Media.model.UploadType;
import com.Media.repository.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaUploadService mediaUploadService;
    private final MediaRepository mediaRepository;

    public Map<String, String> create(List<MultipartFile> files, String productId, UploadType type, String username) {

        // connect with user-service
        if (username == null) {
            throw ApiException.notFound("User not found ");
        }

        // connect to product-service
        // if (productId != null) {
        // throw ApiException.notFound("Product not found ");
        // }

        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("No media uploaded");
        }
        if (type == null || (!type.equals(UploadType.AVATAR) && !type.equals(UploadType.PRODUCT_IMAGE))) {
            throw ApiException.badRequest("Media type is required");
        }

        if (files.size() > 5) {
            throw ApiException.badRequest("Maximum 5 media files allowed");
        }

        List<String> uploadedImageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            try {
                for (MultipartFile file : files) {
                    Media media = new Media();
                    media.setOwnerId(username);
                    media.setType(type);
                    if (productId != null) {
                        media.setProductId(productId);
                    }
                    String url = mediaUploadService.uploadFile(file);
                    uploadedImageUrls.add(url);
                    media.setImagePath(url);
                    mediaRepository.save(media);
                }

            } catch (ApiException e) {
                if (!uploadedImageUrls.isEmpty()) {
                    mediaUploadService.deleteOrphanedFiles(uploadedImageUrls);
                }
                throw e;
            } catch (Exception e) {
                if (!uploadedImageUrls.isEmpty()) {
                    mediaUploadService.deleteOrphanedFiles(uploadedImageUrls);
                }

                throw ApiException.badRequest("Media upload failed. Post creation cancelled.");
            }
        } else {
            Media media = new Media();
            media.setOwnerId(username);
            media.setType(type);
            if (productId != null) {
                media.setProductId(productId);
            }
                                        mediaRepository.save(media);

        }
        return Map.of("message", "success");

    }

}