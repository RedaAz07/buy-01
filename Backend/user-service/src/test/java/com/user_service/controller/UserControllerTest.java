package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.UpdateRequestDTO;
import com.user_service.dto.response.UpdateResponseDTO;
import com.user_service.dto.response.UserResponseDTO;
import com.user_service.exceptions.ApiException;
import com.user_service.model.Roles;
import com.user_service.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("User Controller Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("GET /api/users/me")
    class GetUserControllerTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and user data successfully")
        void shouldGetUserSuccessfully() throws Exception {
            // GIVEN
            UserResponseDTO response = new UserResponseDTO("123", "user", "email@gmail.com", "", Roles.ROLE_CLIENT);
            Principal mockPrincipal = () -> "123";

            when(userService.getMe("123")).thenReturn(response);

            // WHEN & THEN
            mockMvc.perform(get("/api/users/me")
                    .principal(mockPrincipal)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value("123"))
                    .andExpect(jsonPath("$.name").value("user"))
                    .andExpect(jsonPath("$.email").value("email@gmail.com"))
                    .andExpect(jsonPath("$.avatar").value(""))
                    .andExpect(jsonPath("$.role").value("ROLE_CLIENT"));

            verify(userService, times(1)).getMe("123");
        }

        @Test
        @DisplayName("Should return HTTP 401 Unauthorized when user is not found")
        void shouldReturn401WhenUserNotFound() throws Exception {
            // GIVEN
            Principal mockPrincipal = () -> "999";
            when(userService.getMe("999")).thenThrow(ApiException.unauthorized("User not found"));

            // WHEN & THEN
            mockMvc.perform(get("/api/users/me")
                    .principal(mockPrincipal))
                    .andExpect(status().isUnauthorized());

            verify(userService, times(1)).getMe("999");
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me")
    class PutUserControllerTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and updated user data successfully")
        void shouldUpdateUserSuccessfully() throws Exception {
            // GIVEN
            UpdateRequestDTO request = new UpdateRequestDTO("new@gmail.com", "newName");
            UpdateResponseDTO response = new UpdateResponseDTO("newName", "new@gmail.com", "new_jwt_token");
            Principal mockPrincipal = () -> "123";

            when(userService.updateUser(any(UpdateRequestDTO.class), eq("123"))).thenReturn(response);

            // WHEN & THEN
            mockMvc.perform(put("/api/users/me")
                    .principal(mockPrincipal)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))) 
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name").value("newName"))
                    .andExpect(jsonPath("$.email").value("new@gmail.com"))
                    .andExpect(jsonPath("$.jwt").value("new_jwt_token"));

            verify(userService, times(1)).updateUser(any(UpdateRequestDTO.class), eq("123"));
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            // GIVEN
            UpdateRequestDTO request = new UpdateRequestDTO("existing@gmail.com", "newName");
            Principal mockPrincipal = () -> "123";

            when(userService.updateUser(any(UpdateRequestDTO.class), eq("123")))
                    .thenThrow(ApiException.badRequest("Email already exists"));

            // WHEN & THEN
            mockMvc.perform(put("/api/users/me")
                    .principal(mockPrincipal)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))) 
                    .andExpect(status().isBadRequest());

            verify(userService, times(1)).updateUser(any(UpdateRequestDTO.class), eq("123"));
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when DTO validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            // GIVEN 
            UpdateRequestDTO invalidRequest = new UpdateRequestDTO("invalid-email-format", "");
            Principal mockPrincipal = () -> "123";

            // WHEN & THEN
            mockMvc.perform(put("/api/users/me")
                    .principal(mockPrincipal)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }
}