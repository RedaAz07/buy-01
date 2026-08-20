package com.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(@NotBlank String name, @NotBlank String password) {
}