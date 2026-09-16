package com.Media.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

// Removed: import com.Media.dto.response.UploadResult; (Caused the type mismatch)

import com.Media.exceptions.ApiException;
import com.Media.model.Media;
import com.Media.model.UploadType;
import com.Media.repository.MediaRepository;
import com.Media.service.FeignClient.ProductClientInterface;
import com.Media.service.events.MediaEventProducer;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Unit Tests")
class MediaServiceTest {

    @Mock
    private MediaEventProducer mediaEventProducer;
    @Mock
    private MediaUploadService mediaUploadService;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ProductClientInterface productClientInterface;

        @Mock
        private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MediaService mediaService;

    private MockMultipartFile validFile;
    private Media AvatarTest;
    private Media MediaProductTest;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                "test-image-content".getBytes());
        AvatarTest = new Media();
        AvatarTest.setId("123");
        AvatarTest.setImagePath("https://cloudinary.image.png/123");
        AvatarTest.setOwnerId("user123");
        AvatarTest.setPublicId("123456789");
        AvatarTest.setType(UploadType.AVATAR);

        MediaProductTest = new Media();
        MediaProductTest.setId("123");
        MediaProductTest.setImagePath("https://cloudinary.image.png/123");
        MediaProductTest.setOwnerId("user123");
        MediaProductTest.setPublicId("123456789");
        MediaProductTest.setType(UploadType.PRODUCT_IMAGE);
        MediaProductTest.setProductId("123");

    }

    @Nested
    @DisplayName("Input Validations")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NotFound when username is null or blank")
        void shouldThrowWhenUsernameIsInvalid() {
            List<MultipartFile> files = List.of(validFile);

            assertThrows(ApiException.class,
                    () -> mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, "", "user-1", "ROLE_SELLER"));

            assertThrows(ApiException.class, () -> mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, null,
                    "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when files list is empty or null")
        void shouldThrowWhenFilesEmptyOrNull() {
            assertThrows(ApiException.class, () -> mediaService.create(Collections.emptyList(), "prod-1",
                    UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));

            assertThrows(ApiException.class, () -> mediaService.create(null, "prod-1", UploadType.PRODUCT_IMAGE,
                    "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when UploadType is null")
        void shouldThrowWhenTypeIsNull() {
            List<MultipartFile> files = List.of(validFile);
            assertThrows(ApiException.class,
                    () -> mediaService.create(files, "prod-1", null, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when uploading multiple files for AVATAR")
        void shouldThrowWhenMultipleFilesForAvatar() {
            List<MultipartFile> files = List.of(validFile, validFile);
            assertThrows(ApiException.class,
                    () -> mediaService.create(files, null, UploadType.AVATAR, "user1", "user-1", "ROLE_CLIENT"));
        }

        @Test
        @DisplayName("Should throw BadRequest when AVATAR contains a productId")
        void shouldThrowWhenAvatarHasProductId() {
            List<MultipartFile> files = List.of(validFile);
            assertThrows(ApiException.class, () -> mediaService.create(files, "product-123", UploadType.AVATAR, "user1",
                    "user-1", "ROLE_CLIENT"));
        }

        @Test
        @DisplayName("Should throw BadRequest when PRODUCT_IMAGE has missing productId")
        void shouldThrowWhenProductIdIsMissingForProductImage() {
            List<MultipartFile> files = List.of(validFile);
            assertThrows(ApiException.class,
                    () -> mediaService.create(files, "", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when user doesn't own the product")
        void shouldThrowWhenUserDoesNotOwnProduct() {
            List<MultipartFile> files = List.of(validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(false);

            assertThrows(ApiException.class, () -> mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE,
                    "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when uploading more than 5 product images at once")
        void shouldThrowWhenExceedingMaxFilesPerUpload() {
            List<MultipartFile> files = List.of(validFile, validFile, validFile, validFile, validFile, validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(true);

            assertThrows(ApiException.class, () -> mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE,
                    "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when total images (existing + new) exceed quota of 5")
        void shouldThrowWhenTotalQuotaExceeded() {
            List<MultipartFile> files = List.of(validFile, validFile, validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(true);
            when(mediaRepository.countByProductIdAndType("prod-1", UploadType.PRODUCT_IMAGE)).thenReturn(3L);

            assertThrows(ApiException.class, () -> mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE,
                    "seller", "user-1", "ROLE_SELLER"));
        }
    }

    @Nested
    @DisplayName("Avatar Upload Scenarios")
    class AvatarUploadTests {

        @Test
        @DisplayName("Should successfully upload a new Avatar when no previous avatar exists")
        void shouldUploadAvatarSuccessfully() throws Exception {
            List<MultipartFile> files = List.of(validFile);

            MediaUploadService.UploadResult result = new MediaUploadService.UploadResult(
                    "http://cloudinary.com/avatar.jpg", "pub_123");

            Media savedMedia = new Media();
            savedMedia.setOwnerId("user1");
            savedMedia.setImagePath("http://cloudinary.com/avatar.jpg");

            when(mediaRepository.findByOwnerIdAndType("user1", UploadType.AVATAR)).thenReturn(Optional.empty());
            when(mediaUploadService.uploadFile(any(MultipartFile.class))).thenReturn(result);
            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

            List<String> urls = mediaService.create(files, null, UploadType.AVATAR, "user1", "user-1", "ROLE_CLIENT");

            assertEquals(1, urls.size());
            assertEquals("http://cloudinary.com/avatar.jpg", urls.get(0));

            verify(mediaRepository, times(1)).save(any(Media.class));
            verify(mediaEventProducer, times(1)).sendAvatarUploadedEvent("user1", "http://cloudinary.com/avatar.jpg");
            verify(mediaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should upload new avatar and replace/delete old avatar if present")
        void shouldReplaceExistingAvatarSuccessfully() throws Exception {
            List<MultipartFile> files = List.of(validFile);

            MediaUploadService.UploadResult newUpload = new MediaUploadService.UploadResult(
                    "http://cloudinary.com/new_avatar.jpg", "pub_new");

            Media oldAvatar = new Media();
            oldAvatar.setOwnerId("user1");
            oldAvatar.setPublicId("pub_old");
            oldAvatar.setImagePath("http://cloudinary.com/old_avatar.jpg");

            Media savedMedia = new Media();
            savedMedia.setOwnerId("user1");
            savedMedia.setImagePath("http://cloudinary.com/new_avatar.jpg");

            when(mediaRepository.findByOwnerIdAndType("user1", UploadType.AVATAR)).thenReturn(Optional.of(oldAvatar));
            when(mediaUploadService.uploadFile(any(MultipartFile.class))).thenReturn(newUpload);
            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

            List<String> urls = mediaService.create(files, null, UploadType.AVATAR, "user1", "user-1", "ROLE_CLIENT");

            assertEquals("http://cloudinary.com/new_avatar.jpg", urls.get(0));
            verify(mediaRepository, times(1)).delete(oldAvatar);
            verify(mediaEventProducer, times(1)).sendAvatarUploadedEvent("user1",
                    "http://cloudinary.com/new_avatar.jpg");
        }
    }

    @Nested
    @DisplayName("Product Image Upload Scenarios")
    class ProductImageUploadTests {

        @Test
        @DisplayName("Should successfully upload multiple product images and produce Kafka events")
        void shouldUploadProductImagesSuccessfully() throws Exception {
            List<MultipartFile> files = List.of(validFile, validFile);

            MediaUploadService.UploadResult upload1 = new MediaUploadService.UploadResult(
                    "http://cloudinary.com/prod1.jpg", "pub_1");
            MediaUploadService.UploadResult upload2 = new MediaUploadService.UploadResult(
                    "http://cloudinary.com/prod2.jpg", "pub_2");

            Media savedMedia1 = new Media();
            savedMedia1.setProductId("prod-123");
            savedMedia1.setImagePath("http://cloudinary.com/prod1.jpg");

            Media savedMedia2 = new Media();
            savedMedia2.setProductId("prod-123");
            savedMedia2.setImagePath("http://cloudinary.com/prod2.jpg");

            when(productClientInterface.GetUserProduct("prod-123", "user-1", "ROLE_SELLER")).thenReturn(true);
            when(mediaRepository.countByProductIdAndType("prod-123", UploadType.PRODUCT_IMAGE)).thenReturn(1L);
            when(mediaUploadService.uploadFile(any(MultipartFile.class))).thenReturn(upload1).thenReturn(upload2);
            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia1).thenReturn(savedMedia2);

            List<String> urls = mediaService.create(files, "prod-123", UploadType.PRODUCT_IMAGE, "seller", "user-1",
                    "ROLE_SELLER");

            assertEquals(2, urls.size());
            assertEquals("http://cloudinary.com/prod1.jpg", urls.get(0));
            assertEquals("http://cloudinary.com/prod2.jpg", urls.get(1));

            verify(mediaRepository, times(2)).save(any(Media.class));
            verify(mediaEventProducer, times(2)).sendMediaUploadedEvent(eq("prod-123"), anyString());
        }
    }

    @Nested
    @DisplayName("Error Rollback & Exception Handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should catch unexpected upload exception and cleanup uploaded files")
        void shouldCleanupFilesWhenUploadFails() throws Exception {
            List<MultipartFile> files = List.of(validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(true);
            when(mediaRepository.countByProductIdAndType("prod-1", UploadType.PRODUCT_IMAGE)).thenReturn(0L);

            when(mediaUploadService.uploadFile(any())).thenThrow(new RuntimeException("Cloudinary Down"));

            assertThrows(ApiException.class, () -> mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE,
                    "seller", "user-1", "ROLE_SELLER"));

            verify(mediaEventProducer, never()).sendMediaUploadedEvent(any(), any());
        }
    }

    @Nested
    @DisplayName("Get Image Testing")
    class GetMediaServiceTests {

        @Test
        @DisplayName("Should return Media successfully")
        void shouldGetMediaSuccessfully() {
            // Given
            String id = "123";
            Media media = new Media();
            media.setId(id);
            media.setImagePath("http://cloudinary.com/test-image.jpg");

            when(mediaRepository.findById(id)).thenReturn(Optional.of(media));

            // When
            Map<String, String> response = mediaService.getImage(id);

            // Then
            assertNotNull(response);
            assertEquals("http://cloudinary.com/test-image.jpg", response.get("image"));
            verify(mediaRepository, times(1)).findById(id);
        }

        @Test
        @DisplayName("Should return Bad Request when Id is null or blank")
        void shouldReturnBadRequestWhenIdIsNull() {
            // When & Then
            ApiException exceptionNull = assertThrows(
                    ApiException.class,
                    () -> mediaService.getImage(null));
            assertEquals("Media ID is required", exceptionNull.getMessage());

            ApiException exceptionBlank = assertThrows(
                    ApiException.class,
                    () -> mediaService.getImage("   "));
            assertEquals("Media ID is required", exceptionBlank.getMessage());

            verifyNoInteractions(mediaRepository);
        }

        @Test
        @DisplayName("Should return Not Found when media does not exist")
        void shouldReturnNotFound() {
            // Given
            String id = "badId";
            when(mediaRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> mediaService.getImage(id));

            assertEquals("Media not found", exception.getMessage());
            verify(mediaRepository, times(1)).findById(id);
        }
    }

    @Nested
    @DisplayName("Delete Image Testing")
    class DeleteMediaServiceTests {

        @Test
        @DisplayName("Should delete Product Image successfully and publish Kafka event")
        void shouldDeleteProductMediaSuccessfully() throws Exception {
            // Given
            String ownerId = "user123";
            String imageUrl = "http://cloudinary.com/prod1.jpg";
            String publicId = "pub_prod1";
            String productId = "prod_999";

            Media productMedia = new Media();
            productMedia.setId("media_1");
            productMedia.setOwnerId(ownerId);
            productMedia.setImagePath(imageUrl);
            productMedia.setPublicId(publicId);
            productMedia.setProductId(productId);
            productMedia.setType(UploadType.PRODUCT_IMAGE);

            when(mediaRepository.findByImagePathAndOwnerId(imageUrl, ownerId))
                    .thenReturn(Optional.of(productMedia));

            // When
            Map<String, String> response = mediaService.deleteImageByUrl(imageUrl, ownerId);

            // Then
            assertNotNull(response);
            assertEquals(imageUrl, response.get("image"));

            // Verify Mongo Deletion
            verify(mediaRepository, times(1)).delete(productMedia);

            // Verify Cloudinary Deletion
            verify(mediaUploadService, times(1)).deleteFile(publicId);

            // Verify Kafka Event Published
            verify(kafkaTemplate, times(1)).send(eq("media-deleted-topic"), any());
        }

        @Test
        @DisplayName("Should delete Avatar Image successfully without publishing Kafka event")
        void shouldDeleteAvatarMediaWithoutKafkaEvent() throws Exception {
            // Given
            String ownerId = "user123";
            String imageUrl = "http://cloudinary.com/avatar.jpg";
            String publicId = "pub_avatar1";

            Media avatarMedia = new Media();
            avatarMedia.setId("media_2");
            avatarMedia.setOwnerId(ownerId);
            avatarMedia.setImagePath(imageUrl);
            avatarMedia.setPublicId(publicId);
            avatarMedia.setProductId(null);
            avatarMedia.setType(UploadType.AVATAR);

            when(mediaRepository.findByImagePathAndOwnerId(imageUrl, ownerId))
                    .thenReturn(Optional.of(avatarMedia));

            // When
            Map<String, String> response = mediaService.deleteImageByUrl(imageUrl, ownerId);

            // Then
            assertEquals(imageUrl, response.get("image"));
            verify(mediaRepository, times(1)).delete(avatarMedia);
            verify(mediaUploadService, times(1)).deleteFile(publicId);

            // Kafka event must NOT be triggered when productId is null
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("Should throw BadRequest when imageUrl is blank or null")
        void shouldThrowBadRequestWhenImageUrlIsInvalid() {
            assertThrows(ApiException.class, () -> mediaService.deleteImageByUrl("", "user123"));
            assertThrows(ApiException.class, () -> mediaService.deleteImageByUrl(null, "user123"));
            verifyNoInteractions(mediaRepository);
        }

        @Test
        @DisplayName("Should throw NotFound when ownerId is blank or null")
        void shouldThrowNotFoundWhenOwnerIdIsInvalid() {
            assertThrows(ApiException.class, () -> mediaService.deleteImageByUrl("http://image.jpg", ""));
            assertThrows(ApiException.class, () -> mediaService.deleteImageByUrl("http://image.jpg", null));
            verifyNoInteractions(mediaRepository);
        }

        @Test
        @DisplayName("Should throw NotFound when image is not found in database")
        void shouldThrowNotFoundWhenMediaDoesNotExist() {
            // Given
            String imageUrl = "http://cloudinary.com/missing.jpg";
            String ownerId = "user123";

            when(mediaRepository.findByImagePathAndOwnerId(imageUrl, ownerId))
                    .thenReturn(Optional.empty());

            // When & Then
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> mediaService.deleteImageByUrl(imageUrl, ownerId));

            assertEquals("Media not found", exception.getMessage());
            verify(mediaRepository, times(1)).findByImagePathAndOwnerId(imageUrl, ownerId);
            verify(mediaRepository, never()).delete(any());
            verifyNoInteractions(mediaUploadService);
        }

        @Test
        @DisplayName("Should handle Cloudinary exception gracefully and proceed with database deletion")
        void shouldHandleCloudinaryDeletionFailureGracefully() throws Exception {
            // Given
            String ownerId = "user123";
            String imageUrl = "http://cloudinary.com/prod1.jpg";
            String publicId = "pub_prod1";

            Media media = new Media();
            media.setOwnerId(ownerId);
            media.setImagePath(imageUrl);
            media.setPublicId(publicId);

            when(mediaRepository.findByImagePathAndOwnerId(imageUrl, ownerId))
                    .thenReturn(Optional.of(media));
            doThrow(new RuntimeException("Cloudinary Error"))
                    .when(mediaUploadService).deleteFile(publicId);

            // When
            Map<String, String> response = mediaService.deleteImageByUrl(imageUrl, ownerId);

            // error
            assertEquals(imageUrl, response.get("image"));
            verify(mediaRepository, times(1)).delete(media);
            verify(mediaUploadService, times(1)).deleteFile(publicId);
        }
    }
}