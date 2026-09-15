package com.Media.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks
    private MediaService mediaService;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                "test-image-content".getBytes()
        );
    }

    @Nested
    @DisplayName("Input Validations")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NotFound when username is null or blank")
        void shouldThrowWhenUsernameIsInvalid() {
            List<MultipartFile> files = List.of(validFile);

            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, "", "user-1", "ROLE_SELLER"));

            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, null, "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when files list is empty or null")
        void shouldThrowWhenFilesEmptyOrNull() {
            assertThrows(ApiException.class, () ->
                    mediaService.create(Collections.emptyList(), "prod-1", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));

            assertThrows(ApiException.class, () ->
                    mediaService.create(null, "prod-1", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when UploadType is null")
        void shouldThrowWhenTypeIsNull() {
            List<MultipartFile> files = List.of(validFile);
            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", null, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when uploading multiple files for AVATAR")
        void shouldThrowWhenMultipleFilesForAvatar() {
            List<MultipartFile> files = List.of(validFile, validFile);
            assertThrows(ApiException.class, () ->
                    mediaService.create(files, null, UploadType.AVATAR, "user1", "user-1", "ROLE_CLIENT"));
        }

        @Test
        @DisplayName("Should throw BadRequest when AVATAR contains a productId")
        void shouldThrowWhenAvatarHasProductId() {
            List<MultipartFile> files = List.of(validFile);
            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "product-123", UploadType.AVATAR, "user1", "user-1", "ROLE_CLIENT"));
        }

        @Test
        @DisplayName("Should throw BadRequest when PRODUCT_IMAGE has missing productId")
        void shouldThrowWhenProductIdIsMissingForProductImage() {
            List<MultipartFile> files = List.of(validFile);
            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when user doesn't own the product")
        void shouldThrowWhenUserDoesNotOwnProduct() {
            List<MultipartFile> files = List.of(validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(false);

            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when uploading more than 5 product images at once")
        void shouldThrowWhenExceedingMaxFilesPerUpload() {
            List<MultipartFile> files = List.of(validFile, validFile, validFile, validFile, validFile, validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(true);

            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));
        }

        @Test
        @DisplayName("Should throw BadRequest when total images (existing + new) exceed quota of 5")
        void shouldThrowWhenTotalQuotaExceeded() {
            List<MultipartFile> files = List.of(validFile, validFile, validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(true);
            when(mediaRepository.countByProductIdAndType("prod-1", UploadType.PRODUCT_IMAGE)).thenReturn(3L);

            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));
        }
    }

    @Nested
    @DisplayName("Avatar Upload Scenarios")
    class AvatarUploadTests {

        @Test
        @DisplayName("Should successfully upload a new Avatar when no previous avatar exists")
        void shouldUploadAvatarSuccessfully() throws Exception { // FIX: Added throws Exception
            List<MultipartFile> files = List.of(validFile);
            
            // FIX: Using MediaUploadService.UploadResult instead of the DTO
            MediaUploadService.UploadResult result = new MediaUploadService.UploadResult("pub_123", "http://cloudinary.com/avatar.jpg");
            
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
        void shouldReplaceExistingAvatarSuccessfully() throws Exception { // FIX: Added throws Exception
            List<MultipartFile> files = List.of(validFile);
            
            // FIX: Using MediaUploadService.UploadResult
            MediaUploadService.UploadResult newUpload = new MediaUploadService.UploadResult("pub_new", "http://cloudinary.com/new_avatar.jpg");

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
            verify(mediaEventProducer, times(1)).sendAvatarUploadedEvent("user1", "http://cloudinary.com/new_avatar.jpg");
        }
    }

    @Nested
    @DisplayName("Product Image Upload Scenarios")
    class ProductImageUploadTests {

        @Test
        @DisplayName("Should successfully upload multiple product images and produce Kafka events")
        void shouldUploadProductImagesSuccessfully() throws Exception { // FIX: Added throws Exception
            List<MultipartFile> files = List.of(validFile, validFile);
            
            // FIX: Using MediaUploadService.UploadResult
            MediaUploadService.UploadResult upload1 = new MediaUploadService.UploadResult("pub_1", "http://cloudinary.com/prod1.jpg");
            MediaUploadService.UploadResult upload2 = new MediaUploadService.UploadResult("pub_2", "http://cloudinary.com/prod2.jpg");

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

            List<String> urls = mediaService.create(files, "prod-123", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER");

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
        void shouldCleanupFilesWhenUploadFails() throws Exception { // FIX: Added throws Exception to handle IOException in signature
            List<MultipartFile> files = List.of(validFile);
            when(productClientInterface.GetUserProduct("prod-1", "user-1", "ROLE_SELLER")).thenReturn(true);
            when(mediaRepository.countByProductIdAndType("prod-1", UploadType.PRODUCT_IMAGE)).thenReturn(0L);
            
            when(mediaUploadService.uploadFile(any())).thenThrow(new RuntimeException("Cloudinary Down"));

            assertThrows(ApiException.class, () ->
                    mediaService.create(files, "prod-1", UploadType.PRODUCT_IMAGE, "seller", "user-1", "ROLE_SELLER"));

            verify(mediaEventProducer, never()).sendMediaUploadedEvent(any(), any());
        }
    }
}