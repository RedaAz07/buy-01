package com.user_service.mapper;

import org.springframework.stereotype.Component;

import com.user_service.dto.response.AuthResponseDTO;

@Component
public class UserMapper {
    public AuthResponseDTO toDto(String jwt) {
        if (jwt == null) {
            return null;

        }
        return new AuthResponseDTO(jwt);
    }

    // public UserResponseDto userToDto(User user) {
    //     if (user == null) {
    //         return null;
    //     }
    //     return new UserResponseDto(user.getName(), user.getEmail(), user.getId());

    // }
}
