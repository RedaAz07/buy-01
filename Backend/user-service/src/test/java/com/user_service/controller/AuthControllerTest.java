package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.LoginRequestDTO;
import com.user_service.dto.request.RegisterRequestDTO;
import com.user_service.dto.response.AuthResponseDTO;
import com.user_service.exceptions.ApiException;
import com.user_service.model.Roles;
import com.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private AuthResponseDTO authResponseDTO;

    @BeforeEach
    void setUp() {
        authResponseDTO = new AuthResponseDTO("jwt_token_123");
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and JWT  on successful login")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("user", "user123");

            when(userService.login(any(LoginRequestDTO.class))).thenReturn(authResponseDTO);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.jwt").value("jwt_token_123"));

            verify(userService, times(1)).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when validation fail")
        void shouldReturn400WhenDTOValidationFails() throws Exception {
            LoginRequestDTO invalidRequest = new LoginRequestDTO("", "");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when credentials are wrong")
        void shouldReturn400WhenLoginFails() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("wrongUser", "wrongPassword");

            when(userService.login(any(LoginRequestDTO.class)))
                    .thenThrow(ApiException.badRequest("Invalid username or password"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userService, times(1)).login(any(LoginRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and JWT  on successful register")
        void shouldRegisterSuccessfully() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO("user", "user@gmail.com", "user123",
                    Roles.ROLE_CLIENT.name());

            when(userService.register(any(RegisterRequestDTO.class))).thenReturn(authResponseDTO);

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.jwt").value("jwt_token_123"));

            verify(userService, times(1)).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when validation fail")
        void shouldReturn400WhenDTOValidationFails() throws Exception {
            RegisterRequestDTO invalidRequest = new RegisterRequestDTO("", "", "",
                    "INVALID_ROLE");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when email or name already exists")
        void shouldReturn400WhenRegisterFails() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO("user", "user@gmail.com", "user123",
                    Roles.ROLE_CLIENT.name());

            when(userService.register(any(RegisterRequestDTO.class)))
                    .thenThrow(ApiException.badRequest("Email already exists"));

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userService, times(1)).register(any(RegisterRequestDTO.class));
        }

    }
}