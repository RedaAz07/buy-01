package com.user_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mongodb.internal.bulk.UpdateRequest;
import com.user_service.dto.request.LoginRequestDTO;
import com.user_service.dto.request.RegisterRequestDTO;
import com.user_service.dto.request.UpdateRequestDTO;
import com.user_service.dto.response.AuthResponseDTO;
import com.user_service.dto.response.UpdateResponseDTO;
import com.user_service.exceptions.ApiException;
import com.user_service.mapper.UserMapper;
import com.user_service.model.Roles;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import javax.annotation.meta.When;

@ExtendWith(MockitoExtension.class)
@DisplayName("User service test")
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager auth;

    @InjectMocks
    private UserService userService;

    private User mockClientUser;
    private RegisterRequestDTO registerReqTest;
    private AuthResponseDTO authResponseTest;
    private LoginRequestDTO loginReqTest;

    @BeforeEach
    void setUp() {
        mockClientUser = new User();
        mockClientUser.setAvatar("");
        mockClientUser.setEmail("user@gmail.com");
        mockClientUser.setId("123");
        mockClientUser.setName("user");
        mockClientUser.setPassword("hashed_password");
        mockClientUser.setRole(Roles.ROLE_CLIENT);

        registerReqTest = new RegisterRequestDTO("user", "user@gmail.com", "user123", Roles.ROLE_CLIENT.name());
        authResponseTest = new AuthResponseDTO("jwt");
        loginReqTest = new LoginRequestDTO("user", "user123");
    }

    @Nested
    @DisplayName("Register User Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("Should register client user successfully")
        void shouldRegisterSuccessfully() {
            // When
            when(userRepository.existsByName(registerReqTest.name())).thenReturn(false);
            when(userRepository.existsByEmail(registerReqTest.email())).thenReturn(false);
            when(passwordEncoder.encode(registerReqTest.password())).thenReturn("hashed_password");

            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId("123");
                return u;
            });

            when(jwtUtil.generateToken(mockClientUser.getName(), mockClientUser.getRole().name(),
                    mockClientUser.getId())).thenReturn(authResponseTest.jwt());
            when(userMapper.toDto(authResponseTest.jwt())).thenReturn(authResponseTest);

            final AuthResponseDTO result = userService.register(registerReqTest);

            // Then
            assertNotNull(result);
            assertEquals(authResponseTest.jwt(), result.jwt());

            verify(userRepository, times(1)).existsByName(registerReqTest.name());
            verify(userRepository, times(1)).existsByEmail(registerReqTest.email());
            verify(passwordEncoder, times(1)).encode(registerReqTest.password());
            verify(userRepository, times(1))
                    .save(argThat(u -> u.getName().equals(mockClientUser.getName())
                            && u.getPassword().equals("hashed_password")));
        }

        @Test
        @DisplayName("Should throw ApiException when name already exists")
        void shouldThrowExceptionWhenNameAlreadyExists() {
            // When
            when(userRepository.existsByName(registerReqTest.name())).thenReturn(true);

            // Then
            ApiException exception = assertThrows(ApiException.class, () -> userService.register(registerReqTest));
            assertEquals("Name already exists", exception.getMessage());

            verify(userRepository, times(1)).existsByName(registerReqTest.name());
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder, jwtUtil, userMapper);
        }

        @Test
        @DisplayName("Should throw ApiException when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // When
            when(userRepository.existsByName(registerReqTest.name())).thenReturn(false); // Called first
            when(userRepository.existsByEmail(registerReqTest.email())).thenReturn(true); // Called second

            // Then
            ApiException exception = assertThrows(ApiException.class, () -> userService.register(registerReqTest));
            assertEquals("Email already exists", exception.getMessage());

            verify(userRepository, times(1)).existsByName(registerReqTest.name());
            verify(userRepository, times(1)).existsByEmail(registerReqTest.email());
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder, jwtUtil, userMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when role string is invalid")
        void shouldThrowExceptionWhenRoleIsInvalid() {
            // Given
            RegisterRequestDTO invalidRoleRequest = new RegisterRequestDTO("user", "user@gmail.com", "user123",
                    "INVALID_ROLE");

            when(userRepository.existsByName(invalidRoleRequest.name())).thenReturn(false);
            when(userRepository.existsByEmail(invalidRoleRequest.email())).thenReturn(false);
            // Then
            assertThrows(IllegalArgumentException.class, () -> userService.register(invalidRoleRequest));
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder, jwtUtil, userMapper);
        }
    }

    @Nested
    @DisplayName("Login tests")
    class InnerUserServiceTest {
        @Test
        @DisplayName("Test login successfully")
        void shouldLoginSeccessfully() {
            // given
            // when
            when(userRepository.findByName(loginReqTest.name())).thenReturn(Optional.of(mockClientUser));
            when(jwtUtil.generateToken(mockClientUser.getName(), mockClientUser.getRole().name(),
                    mockClientUser.getId())).thenReturn(authResponseTest.jwt());
            when(userMapper.toDto(authResponseTest.jwt())).thenReturn(authResponseTest);

            final AuthResponseDTO result = userService.login(loginReqTest);

            // Then
            assertNotNull(result);
            assertEquals(authResponseTest.jwt(), result.jwt());

            verify(auth, times(1)).authenticate(
                    new UsernamePasswordAuthenticationToken(loginReqTest.name(), loginReqTest.password()));
            verify(userRepository, times(1)).findByName(loginReqTest.name());
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // GIVEN
            when(userRepository.findByName(loginReqTest.name())).thenReturn(Optional.empty());

            // WHEN & THEN
            assertThrows(ApiException.class, () -> userService.login(loginReqTest));

            verify(auth, times(1)).authenticate(any());
            verify(userRepository, times(1)).findByName(loginReqTest.name());
            verifyNoInteractions(jwtUtil, userMapper);
        }

        @Test
        @DisplayName("Should throw exception when password is invalid")
        void shouldThrowExceptionWhenPasswordIsInvalid() {
            // GIVEN - Simulate AuthenticationManager throwing BadCredentialsException
            when(auth.authenticate(any()))
                    .thenThrow(
                            new org.springframework.security.authentication.BadCredentialsException("bad credentials"));

            // WHEN & THEN
            assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                    () -> userService.login(loginReqTest));

            verify(userRepository, never()).findByName(anyString());
            verifyNoInteractions(jwtUtil, userMapper);
        }
    }

   @Nested
    @DisplayName("Update user tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully when new email and name are unique")
        void shouldUpdateUserSuccessfully() {
            // GIVEN
            String userId = "123";
            UpdateRequestDTO request = new UpdateRequestDTO("updatedEmail@gmail.com", "updatedUp");

            when(userRepository.findById(userId)).thenReturn(Optional.of(mockClientUser));
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByName(request.name())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("new_jwt_token");

            // WHEN
            UpdateResponseDTO response = userService.updateUser(request, userId);

            // THEN
            assertNotNull(response);
            assertEquals("updatedUp", response.name());
            assertEquals("updatedEmail@gmail.com", response.email());
            assertEquals("new_jwt_token", response.jwt());

            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userRepository, times(1)).existsByName(request.name());
            verify(userRepository, times(1)).save(mockClientUser);
            verify(jwtUtil, times(1)).generateToken(eq("updatedUp"), eq(mockClientUser.getRole().name()), eq("123"));
        }

        @Test
        @DisplayName("Should skip uniqueness checks when email and name are unchanged")
        void shouldUpdateSuccessfullyWhenFieldsUnchanged() {
            // GIVEN
            String userId = "123";
            // mockClientUser in setUp() already has email "user@gmail.com" and name "user"
            UpdateRequestDTO request = new UpdateRequestDTO("user@gmail.com", "user");

            when(userRepository.findById(userId)).thenReturn(Optional.of(mockClientUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("new_jwt_token");

            // WHEN
            UpdateResponseDTO response = userService.updateUser(request, userId);

            // THEN
            assertNotNull(response);
            // Because email and name match existing user, DB checks are skipped!
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).existsByName(anyString());
            verify(userRepository, times(1)).save(mockClientUser);
        }

        @Test
        @DisplayName("Should throw ApiException when user ID is not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // GIVEN
            String userId = "999";
            UpdateRequestDTO request = new UpdateRequestDTO("updatedEmail@gmail.com", "updatedUp");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // WHEN & THEN
            ApiException exception = assertThrows(ApiException.class, () -> userService.updateUser(request, userId));
            assertEquals("User not found", exception.getMessage());

            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("Should throw ApiException when updated email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // GIVEN
            String userId = "123";
            UpdateRequestDTO request = new UpdateRequestDTO("taken@gmail.com", "updatedUp");

            when(userRepository.findById(userId)).thenReturn(Optional.of(mockClientUser));
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // WHEN & THEN
            ApiException exception = assertThrows(ApiException.class, () -> userService.updateUser(request, userId));
            assertEquals("Email already exists", exception.getMessage());

            verify(userRepository, never()).existsByName(anyString());
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("Should throw ApiException when updated name already exists")
        void shouldThrowExceptionWhenNameAlreadyExists() {
            // GIVEN
            String userId = "123";
            UpdateRequestDTO request = new UpdateRequestDTO("updatedEmail@gmail.com", "takenName");

            when(userRepository.findById(userId)).thenReturn(Optional.of(mockClientUser));
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByName(request.name())).thenReturn(true);

            // WHEN & THEN
            ApiException exception = assertThrows(ApiException.class, () -> userService.updateUser(request, userId));
            assertEquals("Name already exists", exception.getMessage());

            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(jwtUtil);
        }
    }
}