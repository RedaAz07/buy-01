package com.user_service.mapper;

import org.springframework.stereotype.Component;

import com.user_service.dto.response.AuthResponseDTO;
import com.user_service.dto.response.UserResponseDTO;
import com.user_service.model.User;

@Component
public class UserMapper {
    public AuthResponseDTO toDto(String jwt) {
        if (jwt == null) {
            return null;

        }
        return new AuthResponseDTO(jwt);
    }

    public UserResponseDTO userToDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getAvatar());

    }
}
