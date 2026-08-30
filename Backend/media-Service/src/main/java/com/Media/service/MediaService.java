package com.Media.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaEventProducer mediaEventProducer;

    private final MediaUploadService mediaUploadService;

    private final MediaRepository mediaRepository;

    @Transactional
    public Map<String, String> create(
            List<MultipartFile> files,
            String productId,
            UploadType type,
            String username
    ) {

        if (username == null) {
            throw ApiException.notFound("User not found");
        }

        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("No media uploaded");
        }

        if (type == null ||
                (!type.equals(UploadType.AVATAR)
                        && !type.equals(UploadType.PRODUCT_IMAGE))) {

            throw ApiException.badRequest("Media type is required");
        }

        if (type.equals(UploadType.AVATAR) && files.size() > 1) {
            throw ApiException.badRequest(
                    "Avatar can only include one file"
            );
        }

        // TODO: check if the product already has media
        // and make sure the total does not exceed 5

        if (files.size() > 5) {
            throw ApiException.badRequest(
                    "Maximum 5 media files allowed"
            );
        }

        /*
         * IMPORTANT:
         * Find the old avatar BEFORE saving the new one.
         */
        Optional<Media> oldAvatar = Optional.empty();

        if (type.equals(UploadType.AVATAR)) {

            oldAvatar = mediaRepository.findByOwnerIdAndType(
                    username,
                    UploadType.AVATAR
            );
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

                // Upload new file
                UploadResult result =
                        mediaUploadService.uploadFile(file);

                uploadedPublicIds.add(result.publicId());

                media.setImagePath(result.url());

                // Save new media
                mediaRepository.save(media);

                /*
                 * Avatar:
                 * delete the old avatar after the new one
                 * has been successfully uploaded and saved.
                 */
                if (type.equals(UploadType.AVATAR)) {

                    if (oldAvatar.isPresent()) {

                        Media old = oldAvatar.get();

                        // Delete old Mongo media record
                        mediaRepository.delete(old);

                        /*
                         * TODO:
                         * Delete old file from Cloudinary.
                         *
                         * This requires keeping the old
                         * Cloudinary publicId in Media.
                         */
                    }

                    mediaEventProducer.sendAvatarUploadedEvent(
                            media.getOwnerId(),
                            result.url()
                    );

                } else if (productId != null) {

                    mediaEventProducer.sendMediaUploadedEvent(
                            productId,
                            result.url()
                    );
                }
            }

        } catch (Exception e) {

         ;

            if (!uploadedPublicIds.isEmpty()) {

                mediaUploadService.deleteOrphanedFiles(
                        uploadedPublicIds
                );
            }

            throw ApiException.badRequest(
                    "Media upload failed. Post creation cancelled."
            );
        }

        return Map.of("message", "success");
    }

    public Map<String, String> getImage(String id) {

        Media media = mediaRepository.findById(id)
                .orElseThrow(() ->
                        ApiException.notFound("Media not found")
                );

        return Map.of(
                "image",
                media.getImagePath()
        );
    }

    public Map<String, String> deleteImage(
            String id,
            String name
    ) {

        // TODO: get the id of the product from user service

        Media media = mediaRepository
                .findByIdAndOwnerId(id, name)
                .orElseThrow(() ->
                        ApiException.notFound("Media not found")
                );

        mediaRepository.delete(media);

        return Map.of(
                "image",
                media.getImagePath()
        );
    }
}