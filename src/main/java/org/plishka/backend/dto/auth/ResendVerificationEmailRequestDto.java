package org.plishka.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationEmailRequestDto(
        @NotBlank
        @Email(message = "Email must be a valid email address")
        @Size(min = 5, max = 128)
        String email
) {
}
