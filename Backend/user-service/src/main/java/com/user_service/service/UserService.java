package com.user_service.service;

import com.user_service.mapper.UserMapper;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
import com.user_service.security.CostumUserDetails;
import com.user_service.security.JwtUtil;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CostumUserDetails costumUserDetails;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager auth;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            CostumUserDetails costumUserDetails, JwtUtil jwtUtil, UserMapper userMapper, AuthenticationManager auth) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.costumUserDetails = costumUserDetails;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.auth = auth;
    }

    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByName(request.name())) {
            throw ApiException.badRequest("Name already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.badRequest("Email already exists");
        }
        User nUser = new User();
        nUser.setName(request.name());
        nUser.setEmail(request.email());
        nUser.setRole(Roles.valueOf(request.role()));
        nUser.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(nUser);
        final UserDetails userDetails = costumUserDetails.loadUserByUsername(request.name());
        final String jwt = jwtUtil.generateToken(userDetails, nUser.getId());
        return userMapper.toDto(jwt);

    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        auth.authenticate(
                new UsernamePasswordAuthenticationToken(request.name(), request.password()));
        final UserDetails userDetails = costumUserDetails.loadUserByUsername(request.name());
        User user = userRepository.findByName(request.name())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        final String jwt = jwtUtil.generateToken(userDetails, user.getId());
        return userMapper.toDto(jwt);
    }

    public UserResponseDTO getMe(String username) {
        User user = userRepository
                .findByName(username)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatar());
    }

    public UpdateResponseDTO updateUser(UpdateRequestDTO request, String username) {
        User user = userRepository
                .findByName(username)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (userRepository.existsByName(request.name())) {
            throw ApiException.badRequest("Name already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.badRequest("Email already exists");
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.name() != null) {
            user.setName(request.name());
        }
        userRepository.save(user);
        return new UpdateResponseDTO(user.getName(), user.getEmail());
    }
}
