package com.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRequestDTO(@Email String email,
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Name must include just letters and numbers ") @Size(min = 3, max = 15, message = "Name must be between 3 and 15 charachter") String name) {

}
