package com.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "Name is required") @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Role must be either ROLE_CLIENT or ROLE_SELLER") @Size(min = 3, max = 15, message = "Name must be between 3 and 15 characters") String name,

        @NotBlank(message = "Email is required") @Email(message = "Please provide a valid email address") @Size(max = 50, message = "Email must not exceed 50 characters") String email,

        @NotBlank(message = "Password is required") @Size(min = 6, max = 30, message = "Password must be between 6 and 30 characters") String password,

        @NotBlank(message = "Role is required") @Pattern(regexp = "ROLE_CLIENT|ROLE_SELLER", message = "Role must be either ROLE_CLIENT or ROLE_SELLER") String role

) {
}