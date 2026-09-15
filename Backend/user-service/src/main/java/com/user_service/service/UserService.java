package com.user_service.service;

import com.user_service.mapper.UserMapper;

import java.util.Objects;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.user_service.dto.request.LoginRequestDTO;
import com.user_service.dto.request.RegisterRequestDTO;
import com.user_service.dto.request.UpdateRequestDTO;
import com.user_service.dto.response.AuthResponseDTO;
import com.user_service.dto.response.UpdateResponseDTO;
import com.user_service.dto.response.UserResponseDTO;
import com.user_service.exceptions.ApiException;
import com.user_service.model.Roles;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtUtil;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager auth;

    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByName(request.name())) {
            throw ApiException.badRequest("Name already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.badRequest("Email already exists");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(Roles.valueOf(request.role()));
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        final String jwt = jwtUtil.generateToken(user.getName(), user.getRole().name(), user.getId());
        return userMapper.toDto(jwt);

    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        auth.authenticate(
                new UsernamePasswordAuthenticationToken(request.name(), request.password()));
        User user = userRepository.findByName(request.name())
                .orElseThrow(() -> ApiException.unauthorized("bad credentials"));

        final String jwt = jwtUtil.generateToken(user.getName(), user.getRole().name(), user.getId());
        return userMapper.toDto(jwt);
    }

    public UserResponseDTO getMe(String id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatar(),
                user.getRole());
    }

    public UpdateResponseDTO updateUser(UpdateRequestDTO request, String id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (!Objects.equals(user.getEmail(), request.email())
                && userRepository.existsByEmail(request.email())) {
            throw ApiException.badRequest("Email already exists");
        }

        if (!Objects.equals(user.getName(), request.name())
                && userRepository.existsByName(request.name())) {
            throw ApiException.badRequest("Name already exists");
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.name() != null) {
            user.setName(request.name());
        }
        User nUser = userRepository.save(user);

        final String jwt = jwtUtil.generateToken(nUser.getName(), nUser.getRole().name(), nUser.getId());
        return new UpdateResponseDTO(user.getName(), user.getEmail(), jwt);
    }
}
