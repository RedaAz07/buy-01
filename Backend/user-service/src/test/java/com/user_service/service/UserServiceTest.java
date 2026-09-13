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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.user_service.dto.request.RegisterRequestDTO;
import com.user_service.mapper.UserMapper;
import com.user_service.model.Roles;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.security.CostumUserDetails;
import com.user_service.security.JwtUtil;
import static org.mockito.Mockito.*;
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
    private CostumUserDetails costumUserDetails;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager auth;

    @InjectMocks
    private UserService userService;
    private User mockSellerUser;
    private User mockClientUser;

    @BeforeEach
    void setUp() {
        mockSellerUser = User.builder()
                .id("123")
                .name("testuser")
                .email("test@example.com")
                .password("hashed_password")
                .role(Roles.ROLE_SELLER)
                .build();
    }

    @Nested
    class RegisterUserTests {

        @Test
        void shouldRegisterSuccessfully() {
            //Given
            RegisterRequestDTO req = new RegisterRequestDTO("newUser", "newUser@gmail.com", "PASSWORD",
                    Roles.ROLE_SELLER.toString());

            When(userRepository.existsByEmail(req.email())).thenReturn(false);
            when()

        }

        @Test
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Test duplicate email error
        }

        @Test
        void shouldThrowExceptionWhenPasswordIsTooWeak() {
            // Test bad password error
        }
    }
}