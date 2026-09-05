package com.user_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.user_service.model.Roles;

@JsonInclude(Include.NON_NULL)
public record UserResponseDTO(String id, String name, String email, String avatar, Roles role) {

}
