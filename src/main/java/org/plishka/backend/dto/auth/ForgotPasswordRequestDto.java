package org.plishka.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

public record ForgotPasswordRequestDto(
        @NotBlank(message = "Email is required")
        @ValidEmail
        @Size(min = 5, max = 128, message = "Email must be between 5 and 128 characters long")
        String email
) {
}
