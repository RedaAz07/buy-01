package com.user_service.service;

import com.user_service.mapper.UserMapper;
import javax.management.relation.Role;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.user_service.dto.request.RegisterRequestDTO;
import com.user_service.dto.response.AuthResponseDTO;
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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            CostumUserDetails costumUserDetails, JwtUtil jwtUtil, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.costumUserDetails = costumUserDetails;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
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
        final String jwt = jwtUtil.generateToken(userDetails);
        return userMapper.toDto(jwt);

    }

}
