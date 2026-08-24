package com.Media.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.Media.exceptions.ApiException;
import com.Media.model.Media;
import com.Media.model.UploadType;
import com.Media.repository.MediaRepository;
import com.Media.service.MediaUploadService.UploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaUploadService mediaUploadService;
    private final MediaRepository mediaRepository;

    @Transactional
    public Map<String, String> create(List<MultipartFile> files, String productId, UploadType type, String username) {

        if (username == null) {
            throw ApiException.notFound("User not found");
        }

        // TODO: Validate productId with product-service if required

        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("No media uploaded");
        }

        if (type == null || (!type.equals(UploadType.AVATAR) && !type.equals(UploadType.PRODUCT_IMAGE))) {
            throw ApiException.badRequest("Media type is required");
        }

        if (type.equals(UploadType.AVATAR) && files.size() > 1) {
            throw ApiException.badRequest("Avatar can only include one file");
        }

        if (files.size() > 5) {
            throw ApiException.badRequest("Maximum 5 media files allowed");
        }

        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                Media media = new Media();
                media.setOwnerId(username);
                media.setType(type);
                if (productId != null) {
                    media.setProductId(productId);
                }

                UploadResult result = mediaUploadService.uploadFile(file);

                uploadedPublicIds.add(result.publicId());

                media.setImagePath(result.url());
                mediaRepository.save(media);
            }

        } catch (Exception e) {
            log.error("Media upload failed, rolling back Cloudinary uploads...", e);

            if (!uploadedPublicIds.isEmpty()) {
                mediaUploadService.deleteOrphanedFiles(uploadedPublicIds);
            }

            throw ApiException.badRequest("Media upload failed. Post creation cancelled.");
        }

        return Map.of("message", "success");
    }

    public Map<String, String> getImage(String id) {

        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Media not found"));
        return Map.of("image", media.getImagePath());
    }
}