package com.Media.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.Media.model.UploadType;
import com.Media.service.MediaService;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false) 
@DisplayName("MediaController Integration Tests")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    private static final String BASE_URL = "/api/media/images";
    
    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "testUser";
    }

    @Nested
    @DisplayName("Create Media (POST) Testing")
    class CreateMediaTests {

        @Test
        @DisplayName("Should successfully create media and return 200 OK")
        void shouldCreateMediaSuccessfully() throws Exception {
            // Given
            MockMultipartFile file1 = new MockMultipartFile("media", "image1.jpg", "image/jpeg", "img-data-1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("media", "image2.jpg", "image/jpeg", "img-data-2".getBytes());

            List<String> expectedUrls = List.of("http://cloudinary.com/img1", "http://cloudinary.com/img2");

            when(mediaService.create(
                    anyList(),
                    eq("prod-123"),
                    eq(UploadType.PRODUCT_IMAGE),
                    eq("testUser"),
                    eq("user-1"),
                    eq("ROLE_SELLER")
            )).thenReturn(expectedUrls);

            // When & Then
            mockMvc.perform(multipart(BASE_URL)
                            .file(file1)
                            .file(file2)
                            .param("productId", "prod-123")
                            .param("type", "PRODUCT_IMAGE")
                            .header("Authorization", "Bearer some-token")
                            .header("X-Authenticated-UserID", "user-1")
                            .header("X-Authenticated-Roles", "ROLE_SELLER")
                            .principal(mockPrincipal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size()").value(2))
                    .andExpect(jsonPath("$[0]").value("http://cloudinary.com/img1"))
                    .andExpect(jsonPath("$[1]").value("http://cloudinary.com/img2"));

            verify(mediaService, times(1)).create(anyList(), eq("prod-123"), eq(UploadType.PRODUCT_IMAGE), eq("testUser"), eq("user-1"), eq("ROLE_SELLER"));
        }

       
    }

    @Nested
    @DisplayName("Get Image (GET) Testing")
    class GetMediaTests {

        @Test
        @DisplayName("Should successfully retrieve image map by ID")
        void shouldGetImageSuccessfully() throws Exception {
            // Given
            String mediaId = "123";
            Map<String, String> expectedResponse = Map.of("image", "http://cloudinary.com/test.jpg");

            when(mediaService.getImage(mediaId)).thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{id}", mediaId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.image").value("http://cloudinary.com/test.jpg"));

            verify(mediaService, times(1)).getImage(mediaId);
        }
    }

    @Nested
    @DisplayName("Delete Image (DELETE) Testing")
    class DeleteMediaTests {

        @Test
        @DisplayName("Should successfully decode URL, delete media, and return 200 OK")
        void shouldDeleteMediaSuccessfully() throws Exception {
            // Given
            String encodedUrl = "http%3A%2F%2Fcloudinary.com%2Fmy%20image.jpg";
            String expectedDecodedUrl = "http://cloudinary.com/my image.jpg";
            
            Map<String, String> expectedResponse = Map.of("image", expectedDecodedUrl);

            when(mediaService.deleteImageByUrl(expectedDecodedUrl, "testUser"))
                    .thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(delete(BASE_URL)
                            .param("url", encodedUrl)
                            .principal(mockPrincipal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.image").value(expectedDecodedUrl));

            verify(mediaService, times(1)).deleteImageByUrl(expectedDecodedUrl, "testUser");
        }

       
    }
}