package com.Media.service;

import com.Media.dto.event.DeleteMediaEvent;
import com.Media.exceptions.ApiException;
import com.Media.model.Media;
import com.Media.model.UploadType;
import com.Media.repository.MediaRepository;
import com.Media.service.FeignClient.ProductClientInterface;
import com.Media.service.MediaUploadService.UploadResult;
import com.Media.service.events.MediaEventProducer;
import feign.FeignException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

        private static final int MAX_PRODUCT_IMAGES = 5;

        private final MediaEventProducer mediaEventProducer;
        private final MediaUploadService mediaUploadService;
        private final MediaRepository mediaRepository;
        private final ProductClientInterface productClientInterface;

        @Transactional
        public List<String> create(
                        List<MultipartFile> files,
                        String productId,
                        UploadType type,
                        String username,
                        String token) {
                // Validation

                if (username == null || username.isBlank()) {
                        throw ApiException.notFound("User not found");
                }

                if (files == null || files.isEmpty()) {
                        throw ApiException.badRequest("No media uploaded");
                }

                if (type == null) {
                        throw ApiException.badRequest("Media type is required");
                }

                if (type != UploadType.AVATAR && type != UploadType.PRODUCT_IMAGE) {
                        throw ApiException.badRequest("Invalid media type");
                }

                // Avatar validation

                if (type == UploadType.AVATAR) {
                        if (files.size() > 1) {
                                throw ApiException.badRequest("Avatar can only include one file");
                        }

                        if (productId != null && !productId.isBlank()) {
                                throw ApiException.badRequest(
                                                "Avatar cannot be associated with a product");
                        }
                }

                // Product image validation

                if (type == UploadType.PRODUCT_IMAGE) {
                        if (productId == null || productId.isBlank()) {
                                throw ApiException.badRequest(
                                                "Product ID is required for product images");
                        }

                        boolean bool = productClientInterface.GetUserProduct(productId, token);

                        if (!bool) {
                                throw ApiException.badRequest("Product not found or you don't own it");
                        }

                        if (files.size() > MAX_PRODUCT_IMAGES) {
                                throw ApiException.badRequest("Maximum 5 media files allowed");
                        }

                        // Check existing product images
                        long existingImages = mediaRepository.countByProductIdAndType(
                                        productId,
                                        UploadType.PRODUCT_IMAGE);

                        if (existingImages + files.size() > MAX_PRODUCT_IMAGES) {
                                long remaining = MAX_PRODUCT_IMAGES - existingImages;

                                throw ApiException.badRequest(
                                                "Maximum 5 media files allowed. " +
                                                                "You can upload only " +
                                                                Math.max(remaining, 0) +
                                                                " more image(s)");
                        }
                }

                // Find old avatar (do not delete yet)

                Optional<Media> oldAvatar = Optional.empty();

                if (type == UploadType.AVATAR) {
                        oldAvatar = mediaRepository.findByOwnerIdAndType(
                                        username,
                                        UploadType.AVATAR);
                }

                // Upload Loop

                List<String> uploadedPublicIds = new ArrayList<>();
                List<Runnable> pendingEvents = new ArrayList<>();
                List<String> uploadedUrls = new ArrayList<>();

                try {
                        for (MultipartFile file : files) {
                                validateFile(file);

                                // Upload to Cloudinary
                                UploadResult result = mediaUploadService.uploadFile(file);
                                uploadedPublicIds.add(result.publicId());

                                uploadedUrls.add(result.url());
                                // Create Media entity
                                Media media = new Media();
                                media.setOwnerId(username);
                                media.setType(type);
                                media.setImagePath(result.url());
                                media.setPublicId(result.publicId());

                                if (type == UploadType.PRODUCT_IMAGE) {
                                        media.setProductId(productId);
                                }

                                // Save new media
                                Media savedMedia = mediaRepository.save(media);

                                // Queue up Kafka events safely for after the loop
                                if (type == UploadType.AVATAR) {
                                        pendingEvents.add(() -> mediaEventProducer.sendAvatarUploadedEvent(
                                                        savedMedia.getOwnerId(),
                                                        savedMedia.getImagePath()));
                                } else {
                                        pendingEvents.add(() -> mediaEventProducer.sendMediaUploadedEvent(
                                                        savedMedia.getProductId(),
                                                        savedMedia.getImagePath()));
                                }
                        }
                        // 1. Send all Kafka events now that everything succeeded
                        pendingEvents.forEach(Runnable::run);

                        // 2. Safely delete old avatar ONLY after the new upload is 100% successful
                        if (type == UploadType.AVATAR && oldAvatar.isPresent()) {
                                Media oldAvatarMedia = oldAvatar.get();
                                mediaRepository.delete(oldAvatarMedia);
                                deleteCloudinaryFileSafely(oldAvatarMedia);
                        }
                } catch (ApiException e) {
                        cleanupUploadedFiles(uploadedPublicIds);
                        throw e;
                } catch (Exception e) {
                        log.error("Media upload failed for user={}", username, e);
                        cleanupUploadedFiles(uploadedPublicIds);
                        throw ApiException.badRequest(
                                        "Media upload failed. Post creation cancelled.");
                }
                return uploadedUrls;
        }

        // Get image

        public Map<String, String> getImage(String id) {
                if (id == null || id.isBlank()) {
                        throw ApiException.badRequest("Media ID is required");
                }

                Media media = mediaRepository
                                .findById(id)
                                .orElseThrow(() -> ApiException.notFound("Media not found"));

                return Map.of("image", media.getImagePath());
        }

        // Delete image
        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Transactional
        public Map<String, String> deleteImageByUrl(String imageUrl, String ownerId) {
                if (imageUrl == null || imageUrl.isBlank()) {
                        throw ApiException.badRequest("Image URL is required");
                }

                if (ownerId == null || ownerId.isBlank()) {
                        throw ApiException.notFound("User not found");
                }

                // 1. Find by URL instead of ID
                Media media = mediaRepository
                                .findByImagePathAndOwnerId(imageUrl, ownerId)
                                .orElseThrow(() -> ApiException.notFound("Media not found"));

                String productId = media.getProductId(); // Assuming your Media entity stores this

                // 2. Delete Mongo record
                mediaRepository.delete(media);

                // 3. Delete Cloudinary file
                deleteCloudinaryFileSafely(media);

                // 4. Send event to Product Service via Kafka
                if (productId != null) {
                        DeleteMediaEvent event = new DeleteMediaEvent(
                                        media.getOwnerId(),
                                        productId,
                                        imageUrl,
                                        UploadType.PRODUCT_IMAGE);
                        kafkaTemplate.send("media-deleted-topic", event);
                }

                return Map.of("image", imageUrl);
        }

        // File validation

        private void validateFile(MultipartFile file) {
                if (file == null || file.isEmpty()) {
                        throw ApiException.badRequest("Uploaded file cannot be empty");
                }
        }

        // Cloudinary cleanup

        private void deleteCloudinaryFileSafely(Media media) {
                if (media.getPublicId() == null || media.getPublicId().isBlank()) {
                        log.warn(
                                        "Cannot delete Cloudinary file because publicId is missing. mediaId={}",
                                        media.getId());
                        return;
                }

                try {
                        mediaUploadService.deleteFile(media.getPublicId());
                } catch (Exception e) {
                        log.error(
                                        "Failed to delete Cloudinary file. publicId={}, mediaId={}",
                                        media.getPublicId(),
                                        media.getId(),
                                        e);
                }
        }

        // Cleanup newly uploaded files

        private void cleanupUploadedFiles(List<String> uploadedPublicIds) {
                if (uploadedPublicIds.isEmpty()) {
                        return;
                }

                try {
                        mediaUploadService.deleteOrphanedFiles(uploadedPublicIds);
                } catch (Exception e) {
                        log.error(
                                        "Failed to cleanup orphaned Cloudinary files. publicIds={}",
                                        uploadedPublicIds,
                                        e);
                }
        }
}
