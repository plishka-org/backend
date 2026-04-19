package org.plishka.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

public record ResendVerificationEmailRequestDto(
        @NotBlank
        @ValidEmail
        @Size(min = 5, max = 128)
        String email
) {
}
